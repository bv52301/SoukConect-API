import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, Stack, TextField, Typography, Button, CircularProgress, Autocomplete, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions } from '@mui/material';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import PreviewGallery from '../../components/previewgallery';
import { fetchPreview } from '../../lib/apiClient';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { api, Vendor } from '../../lib/apiClient';
import { z } from 'zod';

const schema = z.object({
  name: z.string().min(1),
  description: z.string().max(1000, 'Max 1000 characters').optional().or(z.literal('')),
  email: z.string().email().optional().or(z.literal('')),
  phoneNumber: z.string().optional().or(z.literal('')),
  address1: z.string().optional().or(z.literal('')),
  address2: z.string().optional().or(z.literal('')),
  state: z.string().optional().or(z.literal('')),
  pincode: z.string().optional().or(z.literal('')),
  contactName: z.string().optional().or(z.literal('')),
  image: z.string().url().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function VendorForm({ mode }: { mode: 'create' | 'edit' | 'view' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();
  const listPath = '/vendors';

  const { data } = useQuery({
    queryKey: ['vendor', id],
    queryFn: () => api<Vendor>(`/vendors/${id}`),
    enabled: mode !== 'create' && !!id,
  });

  const { register, handleSubmit, formState: { errors, isDirty }, reset, watch, setValue } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const values = watch();
  const imageValue = values.image;
  const shrinkIfFilled = (value?: string | null) => (typeof value === 'string' && value.trim().length > 0 ? { shrink: true } : undefined);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [items, setItems] = useState<Array<{url:string; mimeType?: string; sizeBytes?: number}>>([]);
  const [supportedCats, setSupportedCats] = useState<string[]>([]);
  const [initialSupportedCats, setInitialSupportedCats] = useState<string[]>([]);
  const [noChangesOpen, setNoChangesOpen] = useState(false);
  const [unsavedOpen, setUnsavedOpen] = useState(false);
  const [afterSaveDestination, setAfterSaveDestination] = useState<'home' | 'list'>('list');

  // Distinct categories from cuisines service
  const catsQuery = useQuery({
    queryKey: ['cuisine-categories'],
    queryFn: async () => {
      const res = await api<Array<{ category: string; imageUrl?: string | null }>>('/cuisines/categories');
      return res.map(r => r.category).filter((s): s is string => !!s);
    },
  });

  useEffect(() => {
    if (data) {
      reset({
        name: data.name || '',
        description: data.description || '',
        email: data.email || '',
        phoneNumber: data.phoneNumber || '',
        address1: data.address1 || '',
        address2: data.address2 || '',
        state: data.state || '',
        pincode: data.pincode || '',
        contactName: data.contactName || '',
        image: data.image || '',
      });
      // Hydrate supported categories from array or map
      const sc: any = data.supportedCategories;
      if (Array.isArray(sc)) {
        const hydrated = sc.filter((s:any)=> typeof s === 'string');
        setSupportedCats(hydrated);
        setInitialSupportedCats(hydrated);
      }
      else if (sc && typeof sc === 'object') {
        const hydrated = Object.keys(sc);
        setSupportedCats(hydrated);
        setInitialSupportedCats(hydrated);
      } else {
        setSupportedCats([]);
        setInitialSupportedCats([]);
      }
    } else {
      setInitialSupportedCats([]);
    }
  }, [data, reset]);

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: Vendor = {
        name: values.name,
        description: blankToUndef(values.description),
        email: blankToUndef(values.email),
        phoneNumber: blankToUndef(values.phoneNumber),
        address1: blankToUndef(values.address1),
        address2: blankToUndef(values.address2),
        state: blankToUndef(values.state),
        pincode: blankToUndef(values.pincode),
        contactName: blankToUndef(values.contactName),
        image: blankToUndef(values.image),
        supportedCategories: supportedCats.length ? supportedCats : undefined,
      };
      if (mode === 'create') {
        return api<Vendor>('/vendors', { method: 'POST', body: JSON.stringify(payload) });
      } else {
        return api<Vendor>(`/vendors/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendors'] });
      navigate(listPath);
      setAfterSaveDestination('list');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async () => api<void>(`/vendors/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendors'] });
      navigate('/vendors');
    },
  });

  const catsChanged = JSON.stringify([...supportedCats].sort()) !== JSON.stringify([...initialSupportedCats].sort());
  const hasUnsavedChanges = isDirty || catsChanged;

  const submitWithDestination = (destination: 'home' | 'list') => {
    setAfterSaveDestination(destination);
    handleSubmit((values) => {
      if (mode === 'edit' && !hasUnsavedChanges) {
        setNoChangesOpen(true);
        return;
      }
      mutate.mutate(values);
    })();
  };

  const handleBack = () => {
    if (mode === 'edit' && hasUnsavedChanges) {
      setUnsavedOpen(true);
    } else {
      navigate(listPath);
    }
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">{mode === 'create' ? 'Create Vendor' : `Edit Vendor #${id}`}</Typography>
        <Button variant="text" onClick={handleBack}>Back</Button>
      </Stack>
      <Card>
        <CardContent>
          <form onSubmit={(e) => { e.preventDefault(); submitWithDestination('list'); }}>
            <Stack spacing={2}>
              <TextField label="Name" {...register('name')} disabled={mode === 'view'} error={!!errors.name} helperText={errors.name?.message} InputLabelProps={shrinkIfFilled(values.name)} />
              <TextField label="Description" {...register('description')} disabled={mode === 'view'} multiline minRows={3} inputProps={{ maxLength: 1000 }} InputLabelProps={shrinkIfFilled(values.description)} />
              <TextField label="Email" {...register('email')} disabled={mode === 'view'} error={!!errors.email} helperText={errors.email?.message} InputLabelProps={shrinkIfFilled(values.email)} />
              <TextField label="Phone" {...register('phoneNumber')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.phoneNumber)} />
              <TextField label="Address 1" {...register('address1')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.address1)} />
              <TextField label="Address 2" {...register('address2')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.address2)} />
              <TextField label="State" {...register('state')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.state)} />
              <TextField label="Pincode" {...register('pincode')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.pincode)} />
              <TextField label="Contact Name" {...register('contactName')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.contactName)} />
              <TextField label="Image URL" {...register('image')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.image)} />
              {imageValue ? (
                <Card variant="outlined">
                  <CardContent sx={{ display: 'flex', justifyContent: 'center' }}>
                    <img
                      src={imageValue}
                      alt="Vendor"
                      style={{ maxHeight: 180, maxWidth: '100%', objectFit: 'contain' }}
                      onError={(ev) => { (ev.currentTarget as HTMLImageElement).style.display = 'none'; }}
                    />
                  </CardContent>
                </Card>
              ) : null}
              {mode === 'edit' && imageValue ? (
                <Button
                  color="error"
                  variant="outlined"
                  onClick={() => {
                    setValue('image', '', { shouldDirty: true });
                    setItems([]);
                  }}
                >
                  Delete Image
                </Button>
              ) : null}
              <Button
                variant="outlined"
                onClick={async () => {
                  if (!imageValue) return;
                  try {
                    const p = await fetchPreview(imageValue);
                    setItems([{ url: p.localUrl, mimeType: p.mimeType, sizeBytes: p.size }]);
                    setPreviewOpen(true);
                  } catch {
                    setItems([{ url: imageValue }]);
                    setPreviewOpen(true);
                  }
                }}
                disabled={!imageValue || mode === 'view'}
              >Preview</Button>
              <PreviewGallery open={previewOpen} onClose={() => setPreviewOpen(false)} items={items.map(it => ({...it, onDelete: () => { setValue('image',''); setPreviewOpen(false); setItems([]); }}))} />
              <Autocomplete
                multiple
                options={catsQuery.data ?? []}
                value={supportedCats}
                onChange={(_, v) => setSupportedCats(v)}
                renderInput={(params) => <TextField {...params} label="Supported Categories" placeholder="Select categories" />}
                disabled={mode === 'view'}
              />
              {mode !== 'view' ? (
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                  <Button type="submit" variant="contained" disabled={mutate.isPending || deleteMutation.isPending} endIcon={mutate.isPending ? <CircularProgress size={16} /> : undefined}>
                    {mode === 'create' ? 'Create' : 'Save'}
                  </Button>
                  {mode === 'edit' && id ? (
                    <Button color="error" variant="outlined" disabled={deleteMutation.isPending || mutate.isPending} onClick={() => { if (window.confirm('Delete this vendor?')) deleteMutation.mutate(); }}>
                      Delete
                    </Button>
                  ) : null}
                </Stack>
              ) : (
                <Button variant="outlined" component={Link} to={`/vendors/${id}`} >
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
          <Button onClick={() => navigate(listPath)}>Back to Vendors</Button>
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

function blankToUndef<T extends string | undefined>(v: T) {
  if (!v) return undefined;
  return (v as unknown as string).trim() === '' ? undefined : v;
}
