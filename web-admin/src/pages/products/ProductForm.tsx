import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, FormControlLabel, Stack, Switch, TextField, Typography, Button, CircularProgress, Divider, IconButton, Autocomplete } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useEffect, useMemo, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { api, Product, Vendor, Cuisine } from '../../lib/apiClient';
import { z } from 'zod';
import PreviewGallery, { PreviewItem } from '../../components/previewgallery';
import { fetchPreview } from '../../lib/apiClient';

const schema = z.object({
  name: z.string().min(1),
  sku: z.string().min(1),
  price: z.coerce.number().nonnegative(),
  vendorId: z.coerce.number().int().positive(),
  available: z.boolean().optional(),
  categoryDetails: z.string().optional().or(z.literal('')),
  schedule: z.string().optional().or(z.literal('')),
  // Derived from selectors; used only for validation
  cdCuisine: z.string().min(1, 'Cuisine is required'),
  cdCategory: z.string().min(1, 'Category is required'),
  cdSubCategory: z.string().optional().or(z.literal('')),
  cdRegion: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function ProductForm({ mode }: { mode: 'create' | 'edit' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();

  const { data } = useQuery({
    queryKey: ['product', id],
    queryFn: () => api<Product>(`/products/${id}`),
    enabled: mode === 'edit' && !!id,
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
    enabled: mode === 'edit' && !!id,
  });

  // Cuisines for Category Details selections
  const cuisinesQuery = useQuery({
    queryKey: ['cuisines'],
    queryFn: () => api<Cuisine[]>(`/cuisines`),
  });
  const [cuisineName, setCuisineName] = useState<string | null>(null);
  const [category, setCategory] = useState<string | null>(null);
  const [subCategory, setSubCategory] = useState<string | null>(null);
  const [regionCategory, setRegionCategory] = useState<string | null>(null);

  const [files, setFiles] = useState<FileList | null>(null);
  const [filePreviews, setFilePreviews] = useState<string[]>([]);
  const [mediaUrls, setMediaUrls] = useState('');
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewItemsState, setPreviewItemsState] = useState<PreviewItem[]>([]);
  // Schedules state
  type WeeklyItem = { day_of_week: string[]; start: string; end: string; stock: number | string; tz: string };
  type DateItem = { date: string; start: string; end: string; stock: number | string; tz: string };
  const [weekly, setWeekly] = useState<WeeklyItem[]>([]);
  const [datesArr, setDatesArr] = useState<DateItem[]>([]);
  const [blackoutArr, setBlackoutArr] = useState<string[]>([]);
  const dowOptions = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

  const { register, handleSubmit, control, formState: { errors }, reset, watch, setValue } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { available: true, cdCuisine: '', cdCategory: '', cdSubCategory: '', cdRegion: '' } });

  useEffect(() => {
    if (data) {
      reset({
        name: data.name,
        sku: data.sku,
        price: data.price,
        vendorId: data.vendorId,
        available: data.available ?? true,
        categoryDetails: data.categoryDetails ? JSON.stringify(data.categoryDetails, null, 2) : '',
        schedule: data.schedule ? JSON.stringify(data.schedule, null, 2) : '',
      });
      // Pre-populate cuisine fields from categoryDetails if present
      try {
        const cd: any = data.categoryDetails || undefined;
        if (cd) {
          const toSN = (v: any): string | null => (v ?? null) as string | null;
          const v1 = toSN(cd.Cuisinename ?? cd.cuisineName);
          const v2 = toSN(cd.Category ?? cd.category);
          const v3 = toSN(cd.SubCategory ?? cd.subcategory);
          const v4 = toSN(cd.regionCategory ?? cd.region);
          setCuisineName(v1); setValue('cdCuisine', v1 ?? '');
          setCategory(v2); setValue('cdCategory', v2 ?? '');
          setSubCategory(v3); setValue('cdSubCategory', v3 ?? '');
          setRegionCategory(v4); setValue('cdRegion', v4 ?? '');
        }
      } catch {}
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
    }
  }, [data, reset]);

  // Auto-update the categoryDetails JSON text area as selectors change
  useEffect(() => {
    const cd = buildCategoryDetails(cuisineName, category, subCategory, regionCategory);
    if (cd) {
      setValue('categoryDetails', JSON.stringify(cd, null, 2), { shouldDirty: true, shouldValidate: false });
    } else {
      // If nothing selected, clear to keep UX clean
      setValue('categoryDetails', '', { shouldDirty: true, shouldValidate: false });
    }
  }, [cuisineName, category, subCategory, regionCategory, setValue]);

  // Auto-update schedule JSON from UI rows
  useEffect(() => {
    const sc = buildSchedule(weekly, datesArr, blackoutArr);
    if (sc) setValue('schedule', JSON.stringify(sc, null, 2), { shouldDirty: true, shouldValidate: false });
    else setValue('schedule', '', { shouldDirty: true, shouldValidate: false });
  }, [weekly, datesArr, blackoutArr, setValue]);

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: Product = {
        name: values.name,
        sku: values.sku,
        price: Number(values.price),
        vendorId: Number(values.vendorId),
        available: values.available,
        categoryDetails: buildCategoryDetails(cuisineName, category, subCategory, regionCategory) ?? parseJsonSafe(values.categoryDetails),
        schedule: parseJsonSafe(values.schedule),
      };
      if (mode === 'create') {
        const created = await api<Product>('/products', { method: 'POST', body: JSON.stringify({ ...payload, media: parseMediaUrls(mediaUrls) }) });
        // After creating, upload any selected files
        if (files && files.length) await uploadFiles(created.id! , files);
        return created;
      } else {
        const updated = await api<Product>(`/products/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
        if (files && files.length) await uploadFiles(Number(id), files);
        return updated;
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['products'] });
      if (mode === 'edit' && id) qc.invalidateQueries({ queryKey: ['product-media', id] });
      navigate('/products');
    },
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h5">{mode === 'create' ? 'Create Product' : `Edit Product #${id}`}</Typography>
      <Card>
        <CardContent>
          <form onSubmit={handleSubmit(v => mutate.mutate(v))}>
            <Stack spacing={2}>
              <TextField label="Name" {...register('name')} error={!!errors.name} helperText={errors.name?.message} />
              <TextField label="SKU" {...register('sku')} error={!!errors.sku} helperText={errors.sku?.message} />
              <TextField label="Price" type="number" inputProps={{ step: '0.01' }} {...register('price')} error={!!errors.price} helperText={errors.price?.message} />
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
                  />
                )}
                control={control}
              />
              <FormControlLabel control={<Switch checked={!!watch('available')} onChange={(_, c) => setValue('available', c)} />} label="Available" />
              <Typography variant="subtitle1">Category Details</Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <Autocomplete<string>
                  sx={{ flex: 1, minWidth: 220 }}
                  options={[...new Set((cuisinesQuery.data || []).map((c: Cuisine) => c.cuisineName).filter((s): s is string => !!s))]}
                  value={cuisineName}
                  onChange={(_, v) => { const nv = (v ?? null) as string | null; setCuisineName(nv); setValue('cdCuisine', v ?? ''); setCategory(null); setValue('cdCategory',''); setSubCategory(null); setValue('cdSubCategory',''); setRegionCategory(null); setValue('cdRegion',''); }}
                  renderInput={(params) => <TextField {...params} label="Cuisine Name" error={!!errors.cdCuisine} helperText={errors.cdCuisine?.message} />}
                />
                <Autocomplete<string>
                  sx={{ flex: 1, minWidth: 220 }}
                  options={[...new Set((cuisinesQuery.data || [])
                    .filter(c => !cuisineName || c.cuisineName === cuisineName)
                    .map(c => c.category)
                    .filter((s): s is string => !!s))]}
                  value={category}
                  onChange={(_, v) => { const nv = (v ?? null) as string | null; setCategory(nv); setValue('cdCategory', v ?? ''); setSubCategory(null); setValue('cdSubCategory',''); setRegionCategory(null); setValue('cdRegion',''); }}
                  renderInput={(params) => <TextField {...params} label="Category" error={!!errors.cdCategory} helperText={errors.cdCategory?.message} />}
                />
              </Stack>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <Autocomplete<string>
                  sx={{ flex: 1, minWidth: 220 }}
                  options={[...new Set((cuisinesQuery.data || [])
                    .filter(c => (!cuisineName || c.cuisineName === cuisineName) && (!category || c.category === category))
                    .map(c => c.subcategory)
                    .filter((s): s is string => !!s))]}
                  value={subCategory}
                  onChange={(_, v) => { const nv = (v ?? null) as string | null; setSubCategory(nv); setValue('cdSubCategory', v ?? ''); setRegionCategory(null); setValue('cdRegion',''); }}
                  renderInput={(params) => <TextField {...params} label="Sub Category" />}
                />
                <Autocomplete<string>
                  sx={{ flex: 1, minWidth: 220 }}
                  options={[...new Set((cuisinesQuery.data || [])
                    .filter(c => (!cuisineName || c.cuisineName === cuisineName) && (!category || c.category === category) && (!subCategory || c.subcategory === subCategory))
                    .map(c => c.region)
                    .filter((s): s is string => !!s))]}
                  value={regionCategory}
                  onChange={(_, v) => { const nv = (v ?? null) as string | null; setRegionCategory(nv); setValue('cdRegion', v ?? ''); }}
                  renderInput={(params) => <TextField {...params} label="Region Category" />}
                />
              </Stack>
              {/* Hidden inputs to tie selector state into RHF for validation */}
              <input type="hidden" {...register('cdCuisine')} />
              <input type="hidden" {...register('cdCategory')} />
              <input type="hidden" {...register('cdSubCategory')} />
              <input type="hidden" {...register('cdRegion')} />

              {/* Keep the original JSON box, but auto-sync it from selectors */}
              <TextField label="categoryDetails (JSON)" {...register('categoryDetails')} multiline minRows={3} placeholder='Optional: advanced JSON override' />
              <Divider />
              <Typography variant="subtitle1">Schedules</Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" variant="outlined" onClick={() => setWeekly(w => [...w, { day_of_week: [], start: '', end: '', stock: 0, tz: 'Asia/Singapore' }])}>+ Weekly</Button>
                <Button size="small" variant="outlined" onClick={() => setDatesArr(d => [...d, { date: '', start: '', end: '', stock: 0, tz: 'Asia/Singapore' }])}>+ Date</Button>
                <Button size="small" variant="outlined" onClick={() => setBlackoutArr(b => [...b, ''])}>+ Blackout</Button>
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
                  <IconButton aria-label="remove" onClick={()=> setWeekly(arr => arr.filter((_,i)=> i!==idx))}><DeleteIcon /></IconButton>
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
                  <IconButton aria-label="remove" onClick={()=> setDatesArr(arr => arr.filter((_,i)=> i!==idx))}><DeleteIcon /></IconButton>
                </Stack>
              ))}

              {/* Blackout dates */}
              {blackoutArr.map((b, idx) => (
                <Stack key={`b-${idx}`} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
                  <TextField sx={{ width: 170 }} type="date" label="Blackout date" value={b} onChange={e=> setBlackoutArr(arr => arr.map((it,i)=> i===idx ? e.target.value : it))} />
                  <IconButton aria-label="remove" onClick={()=> setBlackoutArr(arr => arr.filter((_,i)=> i!==idx))}><DeleteIcon /></IconButton>
                </Stack>
              ))}
              <TextField label="schedule (JSON)" {...register('schedule')} multiline minRows={4} />
              <Divider />
              <Typography variant="subtitle1">Media</Typography>
              {mode === 'create' && (
                <TextField label="Media URLs (one per line)" value={mediaUrls} onChange={e => setMediaUrls(e.target.value)} multiline minRows={3} placeholder="https://...\nhttps://..." />
              )}
              <input type="file" multiple onChange={e => { setFiles(e.target.files); const arr = Array.from(e.target.files || []).map(f => URL.createObjectURL(f)); setFilePreviews(arr); }} />
              <Button
                variant="outlined"
                onClick={async () => {
                  const baseItems: PreviewItem[] = [];
                  // Existing media
                  if (mediaQuery.data) baseItems.push(...mediaQuery.data.map((m:any)=>({
                    url: m.mediaUrl as string,
                    type: m.mediaType==='VIDEO'?'video':'image',
                    title: m.description as string,
                    onDelete: async () => { await api<void>(`/products/${id}/media/${m.id}`, { method: 'DELETE' }); qc.invalidateQueries({ queryKey: ['product-media', id] }); }
                  } as PreviewItem)));
                  // Selected files
                  if (filePreviews && filePreviews.length) baseItems.push(...filePreviews.map(u=>({url:u})));
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
                disabled={!(mediaUrls?.trim() || (mediaQuery.data && mediaQuery.data.length) || (filePreviews && filePreviews.length))}
              >Preview</Button>
              <PreviewGallery open={previewOpen} onClose={() => setPreviewOpen(false)} items={previewItemsState} />
              {mode === 'edit' && mediaQuery.data && (
                <Stack spacing={1}>
                  {mediaQuery.data.map((m: any) => (
                    <Stack key={m.id} direction="row" spacing={2} alignItems="center">
                      {m.mediaType === 'VIDEO' ? (
                        <video src={m.mediaUrl} style={{ maxHeight: 60 }} controls />
                      ) : (
                        <img src={m.mediaUrl} alt={m.description || ''} style={{ maxHeight: 60 }} onError={(ev)=>{(ev.currentTarget as HTMLImageElement).style.display='none';}} />
                      )}
                      <Typography variant="body2" sx={{ flex: 1 }}>{m.mediaType} - {m.mediaUrl}</Typography>
                      <IconButton size="small" aria-label="delete" onClick={async ()=>{ await api<void>(`/products/${id}/media/${m.id}`, { method: 'DELETE' }); qc.invalidateQueries({ queryKey: ['product-media', id] }); }}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                </Stack>
              )}
              <Button type="submit" variant="contained" disabled={mutate.isPending} endIcon={mutate.isPending ? <CircularProgress size={16} /> : undefined}>
                {mode === 'create' ? 'Create' : 'Save'}
              </Button>
            </Stack>
          </form>
        </CardContent>
      </Card>
    </Stack>
  );
}

function parseJsonSafe(s?: string) {
  if (!s) return undefined;
  try { return JSON.parse(s); } catch { return undefined; }
}

function parseMediaUrls(s: string) {
  const lines = s.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
  if (!lines.length) return undefined as unknown as any;
  return lines.map(url => ({ mediaUrl: url }));
}

async function uploadFiles(productId: number, files: FileList) {
  const uploads = Array.from(files).map(async (f) => {
    const fd = new FormData();
    fd.append('file', f);
    await fetch(`/products/${productId}/media/upload`, { method: 'POST', body: fd });
  });
  await Promise.all(uploads);
}

function previewItems(urls: string, existingMedia?: any[], filePreviews?: string[]): PreviewItem[] {
  const items: PreviewItem[] = [];
  if (urls) {
    const arr = urls.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
    items.push(...arr.map(u => ({ url: u })));
  }
  if (existingMedia) {
    items.push(...existingMedia.map(m => {
      const t: 'video' | 'image' = m.mediaType === 'VIDEO' ? 'video' : 'image';
      return { url: m.mediaUrl as string, type: t, title: m.description as string } as PreviewItem;
    }));
  }
  if (filePreviews && filePreviews.length) {
    items.push(...filePreviews.map(u => ({ url: u })));
  }
  return items;
}

function buildCategoryDetails(
  cuisineName?: string | null,
  category?: string | null,
  subCategory?: string | null,
  regionCategory?: string | null
) {
  const obj: any = {};
  if (cuisineName) obj.Cuisinename = cuisineName; // keep original casing used by backend
  if (category) obj.Category = category;
  if (subCategory) obj.SubCategory = subCategory;
  if (regionCategory) obj.regionCategory = regionCategory;
  return Object.keys(obj).length ? obj : undefined;
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
