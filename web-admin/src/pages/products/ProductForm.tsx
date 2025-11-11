import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, FormControlLabel, Stack, Switch, TextField, Typography, Button, CircularProgress, Divider, IconButton, Autocomplete } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useEffect, useMemo, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { api, Product, Vendor } from '../../lib/apiClient';
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

  const [files, setFiles] = useState<FileList | null>(null);
  const [filePreviews, setFilePreviews] = useState<string[]>([]);
  const [mediaUrls, setMediaUrls] = useState('');
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewItemsState, setPreviewItemsState] = useState<PreviewItem[]>([]);

  const { register, handleSubmit, control, formState: { errors }, reset, watch, setValue } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { available: true } });

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
    }
  }, [data, reset]);

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: Product = {
        name: values.name,
        sku: values.sku,
        price: Number(values.price),
        vendorId: Number(values.vendorId),
        available: values.available,
        categoryDetails: parseJsonSafe(values.categoryDetails),
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
              <TextField label="categoryDetails (JSON)" {...register('categoryDetails')} multiline minRows={4} />
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
