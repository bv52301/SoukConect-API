import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, FormControlLabel, Stack, Switch, TextField, Typography, Button, CircularProgress, Divider, IconButton, Autocomplete, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { api, Product, Vendor, Cuisine } from '../../lib/apiClient';
import { z } from 'zod';
import PreviewGallery, { PreviewItem } from '../../components/previewgallery';
import { fetchPreview } from '../../lib/apiClient';

const schema = z.object({
  name: z.string().min(1),
  sku: z.string().min(1),
  price: z.coerce.number().nonnegative(),
  vendorId: z.coerce.number().int().positive(),
  description: z.string().max(1000, 'Max 1000 characters').optional().or(z.literal('')),
  available: z.boolean().optional(),
  categoryDetails: z.string().min(2, 'At least one category is required'),
  schedule: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function ProductForm({ mode }: { mode: 'create' | 'edit' | 'view' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();
  const listPath = '/products';

  const { data } = useQuery({
    queryKey: ['product', id],
    queryFn: () => api<Product>(`/products/${id}`),
    enabled: mode !== 'create' && !!id,
  });

  // Vendors list for selection (remote search by q)
  const [vendorSearch, setVendorSearch] = useState('');
  const [debounced, setDebounced] = useState('');
  useEffect(() => { const t = setTimeout(()=> setDebounced(vendorSearch), 500); return ()=>clearTimeout(t); }, [vendorSearch]);
  const minChars = debounced && debounced.length >= 2;
  const vendorsQuery = useQuery<Vendor[]>({
    queryKey: ['vendors', minChars ? debounced : 'all'],
    queryFn: () => minChars
      ? api<Vendor[]>(`/vendors?q=${encodeURIComponent(debounced)}`)
      : api<Vendor[]>('/vendors'),
    placeholderData: (prev) => prev ?? [],
    staleTime: 10_000,
  });

  // Existing media list for edit mode
  const mediaQuery = useQuery({
    queryKey: ['product-media', id],
    queryFn: () => api<any[]>(`/products/${id}/media`),
    enabled: mode !== 'create' && !!id,
  });
  const mediaItems = useMemo(() => {
    const normalize = (m: any) => ({
      id: m.id,
      mediaType: m.mediaType || m.type || 'IMAGE',
      mediaUrl: m.mediaUrl || m.url,
      description: m.description,
    });
    const fromQuery = (mediaQuery.data || []).map(normalize).filter(m => !!m.mediaUrl);
    if (fromQuery.length) return fromQuery;
    const embedded = (data as any)?.media;
    if (Array.isArray(embedded)) return embedded.map(normalize).filter(m => !!m.mediaUrl);
    return [];
  }, [mediaQuery.data, data]);

  // Cuisines for Category Details selections
  const cuisinesQuery = useQuery({
    queryKey: ['cuisines'],
    queryFn: () => api<Cuisine[]>(`/cuisines`),
  });
  type CategoryEntry = { cuisineName: string | null; categories: string[]; subCategories: string[]; regionCategory: string | null };
  const emptyEntry: CategoryEntry = { cuisineName: null, categories: [], subCategories: [], regionCategory: null };
  const [categoryEntries, setCategoryEntries] = useState<CategoryEntry[]>([emptyEntry]);

  const [mediaUrls, setMediaUrls] = useState('');
  const [initialMediaUrls, setInitialMediaUrls] = useState('');
  const [newMediaUrl, setNewMediaUrl] = useState('');
  const pendingUrlEntries = useMemo(() => {
    const existing = new Set(mediaItems.map(m => m.mediaUrl));
    return mediaUrls
      .split(/\r?\n/)
      .map(s => s.trim())
      .filter(Boolean)
      .filter(u => !existing.has(u));
  }, [mediaUrls, mediaItems]);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewItemsState, setPreviewItemsState] = useState<PreviewItem[]>([]);
  // Schedules state
  type WeeklyItem = { day_of_week: string[]; start: string; end: string; stock: number | string; tz: string };
  type DateItem = { date: string; start: string; end: string; stock: number | string; tz: string };
  const [weekly, setWeekly] = useState<WeeklyItem[]>([]);
  const [datesArr, setDatesArr] = useState<DateItem[]>([]);
  const [blackoutArr, setBlackoutArr] = useState<string[]>([]);
  const [noChangesOpen, setNoChangesOpen] = useState(false);
  const [unsavedOpen, setUnsavedOpen] = useState(false);
  const [afterSaveDestination, setAfterSaveDestination] = useState<'home' | 'list'>('list');
  const dowOptions = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  const categorySyncInitialized = useRef(false);
  const scheduleSyncInitialized = useRef(false);

  const { register, handleSubmit, control, formState: { errors, isDirty }, reset, watch, setValue } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { available: true, categoryDetails: '' } });
  const hasUnsavedChanges = isDirty || mediaUrls.trim() !== initialMediaUrls.trim();
  const values = watch();
  const shrinkIfFilled = (value: unknown) => {
    if (value === undefined || value === null) return undefined;
    const str = typeof value === 'string' ? value : String(value);
    return str.trim().length > 0 ? { shrink: true } : undefined;
  };

  useEffect(() => {
    if (data) {
      reset({
        name: data.name,
        sku: data.sku,
        price: data.price,
        vendorId: data.vendorId,
        description: data.description || '',
        available: data.available ?? true,
        categoryDetails: data.categoryDetails ? JSON.stringify(data.categoryDetails, null, 2) : '',
        schedule: data.schedule ? JSON.stringify(data.schedule, null, 2) : '',
      });
      const embeddedMedia = Array.isArray((data as any)?.media) ? (data as any).media : [];
      const urlsFromMedia = embeddedMedia.map((m: any) => m.mediaUrl || m.url).filter(Boolean).join('\n');
      setMediaUrls(urlsFromMedia);
      setInitialMediaUrls(urlsFromMedia);
      // Pre-populate category selections from categoryDetails (array of objects)
      const normalized = normalizeCategoryDetailsArray(data?.categoryDetails);
      categorySyncInitialized.current = false;
      if (normalized && normalized.length) {
        setCategoryEntries(normalized);
        setValue('categoryDetails', JSON.stringify(toCategoryPayloadArray(normalized), null, 2), { shouldDirty: false, shouldValidate: false });
      }
      // Pre-populate schedules
      try {
        const sc: any = data.schedule || undefined;
        if (sc) {
          if (Array.isArray(sc.weekly_schedules)) {
            setWeekly(sc.weekly_schedules.map((w:any) => ({
              day_of_week: Array.isArray(w.day_of_week) ? w.day_of_week : [],
              start: w.start || '', end: w.end || '', stock: w.stock ?? '', tz: w.tz || 'Asia/Singapore'
            })));
          }
          if (Array.isArray(sc.dates)) {
            setDatesArr(sc.dates.map((d:any) => ({ date: d.date || '', start: d.start || '', end: d.end || '', stock: d.stock ?? '', tz: d.tz || 'Asia/Singapore' })));
          }
          if (Array.isArray(sc.blackout)) {
            setBlackoutArr(sc.blackout.filter((x:any)=>!!x));
          }
        }
      } catch {}
      scheduleSyncInitialized.current = false;
    }
  }, [data, reset]);

  // Auto-update the categoryDetails JSON text area as selectors change
  useEffect(() => {
    const cd = toCategoryPayloadArray(categoryEntries);
    if (cd && cd.length) {
      setValue('categoryDetails', JSON.stringify(cd, null, 2), { shouldDirty: categorySyncInitialized.current, shouldValidate: false });
    } else {
      setValue('categoryDetails', '', { shouldDirty: categorySyncInitialized.current, shouldValidate: false });
    }
    categorySyncInitialized.current = true;
  }, [categoryEntries, setValue]);

  // Auto-update schedule JSON from UI rows
  useEffect(() => {
    const sc = buildSchedule(weekly, datesArr, blackoutArr);
    if (sc) setValue('schedule', JSON.stringify(sc, null, 2), { shouldDirty: scheduleSyncInitialized.current, shouldValidate: false });
    else setValue('schedule', '', { shouldDirty: scheduleSyncInitialized.current, shouldValidate: false });
    scheduleSyncInitialized.current = true;
  }, [weekly, datesArr, blackoutArr, setValue]);

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const categoryPayload = toCategoryPayloadArray(categoryEntries) ?? parseJsonSafe(values.categoryDetails);
      const urlLines = mediaUrls.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
      const existingUrls = mediaItems.map(m => m.mediaUrl).filter(Boolean);
      const newUrls = urlLines.filter(u => !existingUrls.includes(u));

      const postNewMedia = async (productId: number, urls: string[]) => {
        await Promise.all(urls.map(url =>
          api<Product>(`/products/${productId}/media`, {
            method: 'POST',
            body: JSON.stringify({
              product: { id: productId },
              mediaUrl: url,
              mediaType: 'IMAGE',
              storageProvider: 'LOCAL',
            }),
          })
        ));
      };

      const payload: Product = {
        name: values.name,
        sku: values.sku,
        price: Number(values.price),
        vendorId: Number(values.vendorId),
        description: blankToUndef(values.description),
        available: values.available,
        categoryDetails: categoryPayload,
        schedule: parseJsonSafe(values.schedule),
      };
      if (mode === 'create') {
        const created = await api<Product>('/products', { method: 'POST', body: JSON.stringify(payload) });
        if (created?.id && newUrls.length) await postNewMedia(created.id, newUrls);
        return created;
      } else {
        const updated = await api<Product>(`/products/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
        if (id && newUrls.length) await postNewMedia(Number(id), newUrls);
        return updated;
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['products'] });
      if (mode === 'edit' && id) {
        qc.invalidateQueries({ queryKey: ['product-media', id] });
        qc.invalidateQueries({ queryKey: ['product', id] });
      }
      navigate(listPath);
      setAfterSaveDestination('list');
    },
  });

  const submitWithDestination = (destination: 'home' | 'list') => {
    setAfterSaveDestination(destination);
    handleSubmit((formValues) => {
      if (mode === 'edit' && !hasUnsavedChanges) {
        setNoChangesOpen(true);
        return;
      }
      mutate.mutate(formValues);
    })();
  };

  const handleBack = () => {
    if (mode === 'edit' && hasUnsavedChanges) {
      setUnsavedOpen(true);
    } else {
      navigate(listPath);
    }
  };
  const handleAddUrl = () => {
    const url = newMediaUrl.trim();
    if (!url) return;
    setMediaUrls(prev => {
      const trimmed = prev.trim();
      return trimmed ? `${trimmed}\n${url}` : url;
    });
    setNewMediaUrl('');
  };
  const handleRemoveUrl = (url: string) => {
    const lines = mediaUrls.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    setMediaUrls(lines.filter(l => l !== url).join('\n'));
    setInitialMediaUrls(prev => prev.split(/\r?\n/).map(l=>l.trim()).filter(Boolean).filter(l=> l !== url).join('\n'));
  };
  const handleDeleteExistingMedia = async (mediaId?: number, mediaUrl?: string) => {
    if (!id || !mediaId) return;
    await api<void>(`/products/${id}/media/${mediaId}`, { method: 'DELETE' });
    qc.invalidateQueries({ queryKey: ['product-media', id] });
    qc.invalidateQueries({ queryKey: ['product', id] });
    if (mediaUrl) handleRemoveUrl(mediaUrl);
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">{mode === 'create' ? 'Create Product' : `Edit Product #${id}`}</Typography>
        <Button variant="text" onClick={handleBack}>Back</Button>
      </Stack>
      <Card>
        <CardContent>
          <form onSubmit={(e) => { e.preventDefault(); submitWithDestination('list'); }}>
            <Stack spacing={2}>
              <TextField label="Name" {...register('name')} disabled={mode === 'view'} error={!!errors.name} helperText={errors.name?.message} InputLabelProps={shrinkIfFilled(values.name)} />
              <TextField label="SKU" {...register('sku')} disabled={mode === 'view'} error={!!errors.sku} helperText={errors.sku?.message} InputLabelProps={shrinkIfFilled(values.sku)} />
              <TextField label="Price" type="number" inputProps={{ step: '0.01' }} {...register('price')} disabled={mode === 'view'} error={!!errors.price} helperText={errors.price?.message} InputLabelProps={shrinkIfFilled(values.price)} />
              <TextField label="Description" {...register('description')} disabled={mode === 'view'} multiline minRows={3} inputProps={{ maxLength: 1000 }} InputLabelProps={shrinkIfFilled(values.description)} />
              <Controller
                name="vendorId"
                render={({ field }) => (
                  <Autocomplete
                    options={vendorsQuery.data ?? []}
                    loading={vendorsQuery.isLoading}
                    getOptionLabel={(v: Vendor) => `${v.vendorId ?? ''} - ${v.name ?? ''}${v.email ? ` (${v.email})` : ''}`}
                    value={(vendorsQuery.data ?? []).find((v: Vendor) => v.vendorId === Number(field.value)) || null}
                    onChange={(_, val) => field.onChange(val?.vendorId ?? '')}
                    onInputChange={(_, val, reason) => { if (reason === 'input') setVendorSearch(val); }}
                    isOptionEqualToValue={(o, v) => o.vendorId === v.vendorId}
                    renderInput={(params) => (
                      <TextField {...params} label="Vendor" error={!!errors.vendorId} helperText={errors.vendorId?.message || 'Type to search; showing default list'} placeholder="Search vendor" />
                    )}
                    disabled={mode === 'view'}
                  />
                )}
                control={control}
              />
              <FormControlLabel control={<Switch checked={!!watch('available')} onChange={(_, c) => setValue('available', c)} disabled={mode === 'view'} />} label="Available" />
              <Typography variant="subtitle1">Category Details</Typography>
              <Stack spacing={2}>
                {categoryEntries.map((entry, idx) => (
                  <Stack key={`cat-entry-${idx}`} spacing={2} border={1} borderColor="divider" borderRadius={1} padding={2}>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
                    <Autocomplete<string>
                      sx={{ flex: 1, minWidth: 220 }}
                      options={[...new Set((cuisinesQuery.data || []).map((c: Cuisine) => c.cuisineName).filter((s): s is string => !!s))]}
                      value={entry.cuisineName}
                      onChange={(_, v) => {
                        setCategoryEntries(arr => arr.map((it, i) => i === idx ? { ...it, cuisineName: v ?? null, categories: [], subCategories: [], regionCategory: null } : it));
                      }}
                      renderInput={(params) => <TextField {...params} label="Cuisine Name" error={idx === 0 && !!errors.categoryDetails} helperText={idx === 0 ? errors.categoryDetails?.message : undefined} />}
                    />
                    <IconButton aria-label="remove" onClick={() => setCategoryEntries(arr => arr.filter((_, i) => i !== idx))} disabled={categoryEntries.length === 1}>
                      <DeleteIcon />
                    </IconButton>
                  </Stack>
                    <Autocomplete<string, true, false, false>
                      multiple
                      sx={{ minWidth: 220 }}
                      options={[...new Set((cuisinesQuery.data || [])
                        .filter(c => !entry.cuisineName || c.cuisineName === entry.cuisineName)
                        .map(c => c.category)
                        .filter((s): s is string => !!s))]}
                      value={entry.categories}
                      onChange={(_, v) => {
                        setCategoryEntries(arr => arr.map((it, i) => i === idx ? { ...it, categories: v, subCategories: it.subCategories.filter(s => v.includes(findCategoryForSub(cuisinesQuery.data || [], it.cuisineName, s) ?? '')) } : it));
                      }}
                      renderInput={(params) => <TextField {...params} label="Categories" placeholder="Select categories" />}
                    />
                    <Autocomplete<string, true, false, false>
                      multiple
                      sx={{ minWidth: 220 }}
                      options={[...new Set((cuisinesQuery.data || [])
                        .filter(c => (!entry.cuisineName || c.cuisineName === entry.cuisineName) && (entry.categories.length === 0 || entry.categories.includes(c.category || '')))
                        .map(c => c.subcategory)
                        .filter((s): s is string => !!s))]}
                      value={entry.subCategories}
                      onChange={(_, v) => setCategoryEntries(arr => arr.map((it, i) => i === idx ? { ...it, subCategories: v } : it))}
                      renderInput={(params) => <TextField {...params} label="Sub Categories" placeholder="Optional" />}
                    />
                    <Autocomplete<string>
                      sx={{ minWidth: 220 }}
                      options={[...new Set((cuisinesQuery.data || [])
                        .filter(c => (!entry.cuisineName || c.cuisineName === entry.cuisineName) && (entry.categories.length === 0 || entry.categories.includes(c.category || '')))
                        .map(c => c.region)
                        .filter((s): s is string => !!s))]}
                      value={entry.regionCategory}
                      onChange={(_, v) => setCategoryEntries(arr => arr.map((it, i) => i === idx ? { ...it, regionCategory: v ?? null } : it))}
                      renderInput={(params) => <TextField {...params} label="Region Category" placeholder="Optional" />}
                    />
                  </Stack>
                ))}
                <Button size="small" variant="outlined" onClick={() => setCategoryEntries(arr => [...arr, emptyEntry])}>
                  + Add Category Group
                </Button>
              </Stack>

              {/* Keep the original JSON box, but auto-sync it from selectors */}
              <TextField label="categoryDetails (JSON)" {...register('categoryDetails')} multiline minRows={3} placeholder='Optional: advanced JSON override' InputLabelProps={shrinkIfFilled(values.categoryDetails)} />
              <Divider />
              <Typography variant="subtitle1">Schedules</Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" variant="outlined" onClick={() => setWeekly(w => [...w, { day_of_week: [], start: '', end: '', stock: 0, tz: 'Asia/Singapore' }])} disabled={mode === 'view'}>+ Weekly</Button>
                <Button size="small" variant="outlined" onClick={() => setDatesArr(d => [...d, { date: '', start: '', end: '', stock: 0, tz: 'Asia/Singapore' }])} disabled={mode === 'view'}>+ Date</Button>
                <Button size="small" variant="outlined" onClick={() => setBlackoutArr(b => [...b, ''])} disabled={mode === 'view'}>+ Blackout</Button>
              </Stack>

              {/* Weekly rows */}
              {weekly.map((w, idx) => (
                <Stack key={`w-${idx}`} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
                  <Autocomplete<string, true, false, false>
                    multiple
                    sx={{ minWidth: 220, flex:1 }}
                    options={dowOptions}
                    value={w.day_of_week}
                    onChange={(_, v) => setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, day_of_week: v } : it))}
                    renderInput={(p)=> <TextField {...p} label="Days of week" />}
                  />
                  <TextField sx={{ width: 130 }} type="time" label="Start" value={w.start} onChange={e=> setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, start: e.target.value } : it))} />
                  <TextField sx={{ width: 130 }} type="time" label="End" value={w.end} onChange={e=> setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, end: e.target.value } : it))} />
                  <TextField sx={{ width: 120 }} type="number" label="Stock" value={w.stock} onChange={e=> setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, stock: e.target.value } : it))} />
                  <TextField sx={{ minWidth: 180 }} label="Timezone" value={w.tz} onChange={e=> setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, tz: e.target.value } : it))} />
                  <IconButton aria-label="remove" onClick={()=> setWeekly(arr => arr.filter((_,i)=> i!==idx))} disabled={mode === 'view'}><DeleteIcon /></IconButton>
                </Stack>
              ))}

              {/* Date rows */}
              {datesArr.map((d, idx) => (
                <Stack key={`d-${idx}`} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
                  <TextField sx={{ width: 170 }} type="date" label="Date" value={d.date} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, date: e.target.value } : it))} />
                  <TextField sx={{ width: 130 }} type="time" label="Start" value={d.start} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, start: e.target.value } : it))} />
                  <TextField sx={{ width: 130 }} type="time" label="End" value={d.end} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, end: e.target.value } : it))} />
                  <TextField sx={{ width: 120 }} type="number" label="Stock" value={d.stock} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, stock: e.target.value } : it))} />
                  <TextField sx={{ minWidth: 180 }} label="Timezone" value={d.tz} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, tz: e.target.value } : it))} />
                  <IconButton aria-label="remove" onClick={()=> setDatesArr(arr => arr.filter((_,i)=> i!==idx))} disabled={mode === 'view'}><DeleteIcon /></IconButton>
                </Stack>
              ))}

              {/* Blackout dates */}
              {blackoutArr.map((b, idx) => (
                <Stack key={`b-${idx}`} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
                  <TextField sx={{ width: 170 }} type="date" label="Blackout date" value={b} onChange={e=> setBlackoutArr(arr => arr.map((it,i)=> i===idx ? e.target.value : it))} />
                  <IconButton aria-label="remove" onClick={()=> setBlackoutArr(arr => arr.filter((_,i)=> i!==idx))} disabled={mode === 'view'}><DeleteIcon /></IconButton>
                </Stack>
              ))}
              <TextField label="schedule (JSON)" {...register('schedule')} multiline minRows={4} InputLabelProps={shrinkIfFilled(values.schedule)} />
              <Divider />
              <Typography variant="subtitle1">Media</Typography>
              {mode !== 'view' && (
                <TextField label="Media URLs (one per line)" value={mediaUrls} onChange={e => setMediaUrls(e.target.value)} multiline minRows={3} placeholder="https://...\nhttps://..." />
              )}
              {mode !== 'view' && (
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ xs: 'stretch', sm: 'center' }}>
                  <TextField label="Add media URL" value={newMediaUrl} onChange={e => setNewMediaUrl(e.target.value)} placeholder="https://..." fullWidth />
                  <Button variant="outlined" onClick={handleAddUrl} disabled={!newMediaUrl.trim()}>+ Add</Button>
                </Stack>
              )}
              <Button
                variant="outlined"
                onClick={async () => {
                  const baseItems: PreviewItem[] = [];
                  // Existing media (from query or embedded)
                  if (mediaItems.length) baseItems.push(...mediaItems.map((m:any)=>({
                    url: m.mediaUrl as string,
                    type: m.mediaType==='VIDEO'?'video':'image',
                    title: m.description as string,
                    onDelete: async () => { await handleDeleteExistingMedia(m.id, m.mediaUrl); }
                  } as PreviewItem)));
                  // URLs entered (fetch copies via backend)
                  const urls = mediaUrls.split(/\r?\n/).map(s=>s.trim()).filter(Boolean);
                  const fetched: PreviewItem[] = [];
                  for (const u of urls) {
                    try { const p = await fetchPreview(u); fetched.push({ url: p.localUrl, mimeType: p.mimeType, sizeBytes: p.size }); } catch { fetched.push({ url: u }); }
                  }
                  // Show existing media first so Delete action is visible immediately when applicable
                  const all = [...baseItems, ...fetched];
                  setPreviewItemsState(all);
                  setPreviewOpen(true);
                }}
                disabled={mode === 'view' || !(mediaUrls?.trim() || mediaItems.length)}
              >Preview</Button>
              <PreviewGallery open={previewOpen} onClose={() => setPreviewOpen(false)} items={previewItemsState} />
              {mode !== 'create' && mediaItems.length > 0 && (
                <Stack spacing={1}>
                  {mediaItems.map((m: any) => (
                    <Stack key={m.id ?? m.mediaUrl} direction="row" spacing={2} alignItems="center">
                      {m.mediaType === 'VIDEO' ? (
                        <video src={m.mediaUrl} style={{ maxHeight: 60 }} controls />
                      ) : (
                        <img src={m.mediaUrl} alt={m.description || ''} style={{ maxHeight: 60 }} onError={(ev)=>{(ev.currentTarget as HTMLImageElement).style.display='none';}} />
                      )}
                      <Typography variant="body2" sx={{ flex: 1 }}>{m.mediaType} - {m.mediaUrl}</Typography>
                      <IconButton size="small" aria-label="delete" onClick={async ()=>{ await handleDeleteExistingMedia(m.id, m.mediaUrl); }} disabled={mode === 'view'}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                </Stack>
              )}
              {mode !== 'view' && pendingUrlEntries.length > 0 && (
                <Stack spacing={1}>
                  {pendingUrlEntries.map((url, idx) => (
                    <Stack key={`${url}-${idx}`} direction="row" spacing={2} alignItems="center">
                      <img src={url} alt={url} style={{ maxHeight: 60, maxWidth: 80, objectFit: 'contain' }} onError={(ev)=>{(ev.currentTarget as HTMLImageElement).style.display='none';}} />
                      <Typography variant="body2" sx={{ flex: 1 }}>New - {url}</Typography>
                      <IconButton size="small" aria-label="delete" onClick={() => handleRemoveUrl(url)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                </Stack>
              )}
              {mode !== 'view' ? (
                <Button type="submit" variant="contained" disabled={mutate.isPending} endIcon={mutate.isPending ? <CircularProgress size={16} /> : undefined}>
                  {mode === 'create' ? 'Create' : 'Save'}
                </Button>
              ) : (
                <Button variant="outlined" component={Link} to={`/products/${id}`} >
                  Edit
                </Button>
              )}
            </Stack>
          </form>
        </CardContent>
      </Card>
      <Dialog open={noChangesOpen} onClose={() => setNoChangesOpen(false)}>
        <DialogTitle>No items edited</DialogTitle>
        <DialogContent>
          <DialogContentText>No fields were changed. Nothing to save.</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNoChangesOpen(false)}>Close</Button>
          <Button onClick={() => navigate(listPath)}>Back to Products</Button>
        </DialogActions>
      </Dialog>
      <Dialog open={unsavedOpen} onClose={() => setUnsavedOpen(false)}>
        <DialogTitle>There are unsaved items</DialogTitle>
        <DialogContent>
          <DialogContentText>There are unsaved items do you want to save them?</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setUnsavedOpen(false); navigate(listPath); }}>No</Button>
          <Button onClick={() => { setUnsavedOpen(false); submitWithDestination('home'); }}>Yes</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

function parseJsonSafe(s?: string) {
  if (!s) return undefined;
  try { return JSON.parse(s); } catch { return undefined; }
}

function toCategoryPayloadArray(entries: { cuisineName: string | null; categories: string[]; subCategories: string[]; regionCategory: string | null }[]) {
  const arr = entries
    .filter(e => e.cuisineName && e.categories && e.categories.length > 0)
    .map(e => ({
      Cuisinename: e.cuisineName,
      Category: e.categories,
      SubCategory: e.subCategories,
      ...(e.regionCategory ? { regionCategory: e.regionCategory } : {})
    }));
  return arr.length ? arr : undefined;
}

function buildSchedule(
  weekly: { day_of_week: string[]; start: string; end: string; stock: number | string; tz: string }[],
  datesArr: { date: string; start: string; end: string; stock: number | string; tz: string }[],
  blackoutArr: string[]
) {
  const sc: any = {};
  const w = weekly.filter(x => x.day_of_week.length && x.start && x.end).map(x => ({
    day_of_week: x.day_of_week,
    start: x.start,
    end: x.end,
    stock: Number(x.stock) || 0,
    tz: x.tz || 'Asia/Singapore'
  }));
  if (w.length) sc.weekly_schedules = w;
  const d = datesArr.filter(x => x.date && x.start && x.end).map(x => ({
    date: x.date,
    start: x.start,
    end: x.end,
    stock: Number(x.stock) || 0,
    tz: x.tz || 'Asia/Singapore'
  }));
  if (d.length) sc.dates = d;
  const b = blackoutArr.filter(Boolean);
  if (b.length) sc.blackout = b;
  return Object.keys(sc).length ? sc : undefined;
}

function blankToUndef<T extends string | undefined>(v: T) {
  if (!v) return undefined;
  return (v as unknown as string).trim() === '' ? undefined : v;
}

function normalizeCategoryDetailsArray(raw?: unknown) {
  if (!raw) return undefined;
  const arr = Array.isArray(raw) ? raw : [raw as any];
  const entries = arr
    .map(item => {
      if (!item || typeof item !== 'object') return undefined;
      const cuisineName = (item.Cuisinename ?? item.cuisineName ?? null) as string | null;
      const cat = item.Category;
      const sub = item.SubCategory;
      return {
        cuisineName,
        categories: Array.isArray(cat) ? cat.map(String) : [],
        subCategories: Array.isArray(sub) ? sub.map(String) : [],
        regionCategory: (item.regionCategory ?? item.region ?? null) as string | null,
      };
    })
    .filter((e): e is { cuisineName: string | null; categories: string[]; subCategories: string[]; regionCategory: string | null } => !!e);
  return entries.length ? entries : undefined;
}

function findCategoryForSub(cuisines: Cuisine[], cuisineName: string | null, sub: string) {
  const match = cuisines.find(c => (!cuisineName || c.cuisineName === cuisineName) && c.subcategory === sub);
  return match?.category;
}
