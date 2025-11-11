import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, Stack, TextField, Typography, Button, CircularProgress } from '@mui/material';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import PreviewGallery from '../../components/previewgallery';
import { fetchPreview } from '../../lib/apiClient';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { api, Vendor } from '../../lib/apiClient';
import { z } from 'zod';

const schema = z.object({
  name: z.string().min(1),
  email: z.string().email().optional().or(z.literal('')),
  phoneNumber: z.string().optional().or(z.literal('')),
  address1: z.string().optional().or(z.literal('')),
  address2: z.string().optional().or(z.literal('')),
  state: z.string().optional().or(z.literal('')),
  pincode: z.string().optional().or(z.literal('')),
  contactName: z.string().optional().or(z.literal('')),
  image: z.string().url().optional().or(z.literal('')),
  supportedCategories: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function VendorForm({ mode }: { mode: 'create' | 'edit' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();

  const { data } = useQuery({
    queryKey: ['vendor', id],
    queryFn: () => api<Vendor>(`/vendors/${id}`),
    enabled: mode === 'edit' && !!id,
  });

  const { register, handleSubmit, formState: { errors }, reset, watch, setValue } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const [previewOpen, setPreviewOpen] = useState(false);
  const [items, setItems] = useState<Array<{url:string; mimeType?: string; sizeBytes?: number}>>([]);

  useEffect(() => {
    if (data) {
      reset({
        name: data.name || '',
        email: data.email || '',
        phoneNumber: data.phoneNumber || '',
        address1: data.address1 || '',
        address2: data.address2 || '',
        state: data.state || '',
        pincode: data.pincode || '',
        contactName: data.contactName || '',
        image: data.image || '',
        supportedCategories: data.supportedCategories ? JSON.stringify(data.supportedCategories, null, 2) : '',
      });
    }
  }, [data, reset]);

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: Vendor = {
        name: values.name,
        email: blankToUndef(values.email),
        phoneNumber: blankToUndef(values.phoneNumber),
        address1: blankToUndef(values.address1),
        address2: blankToUndef(values.address2),
        state: blankToUndef(values.state),
        pincode: blankToUndef(values.pincode),
        contactName: blankToUndef(values.contactName),
        image: blankToUndef(values.image),
        supportedCategories: parseJsonSafe(values.supportedCategories),
      };
      if (mode === 'create') {
        return api<Vendor>('/vendors', { method: 'POST', body: JSON.stringify(payload) });
      } else {
        return api<Vendor>(`/vendors/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendors'] });
      navigate('/vendors');
    },
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h5">{mode === 'create' ? 'Create Vendor' : `Edit Vendor #${id}`}</Typography>
      <Card>
        <CardContent>
          <form onSubmit={handleSubmit(v => mutate.mutate(v))}>
            <Stack spacing={2}>
              <TextField label="Name" {...register('name')} error={!!errors.name} helperText={errors.name?.message} />
              <TextField label="Email" {...register('email')} error={!!errors.email} helperText={errors.email?.message} />
              <TextField label="Phone" {...register('phoneNumber')} />
              <TextField label="Address 1" {...register('address1')} />
              <TextField label="Address 2" {...register('address2')} />
              <TextField label="State" {...register('state')} />
              <TextField label="Pincode" {...register('pincode')} />
              <TextField label="Contact Name" {...register('contactName')} />
              <TextField label="Image URL" {...register('image')} />
              <Button
                variant="outlined"
                onClick={async () => {
                  if (!watch('image')) return;
                  try {
                    const p = await fetchPreview(watch('image')!);
                    setItems([{ url: p.localUrl, mimeType: p.mimeType, sizeBytes: p.size }]);
                    setPreviewOpen(true);
                  } catch {
                    setItems([{ url: watch('image')! }]);
                    setPreviewOpen(true);
                  }
                }}
                disabled={!watch('image')}
              >Preview</Button>
              <PreviewGallery open={previewOpen} onClose={() => setPreviewOpen(false)} items={items.map(it => ({...it, onDelete: () => { setValue('image',''); setPreviewOpen(false); setItems([]); }}))} />
              <TextField label="supportedCategories (JSON)" {...register('supportedCategories')}
                multiline minRows={6} placeholder='{"Category": "..."}' />
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

function blankToUndef<T extends string | undefined>(v: T) {
  if (!v) return undefined;
  return (v as unknown as string).trim() === '' ? undefined : v;
}
