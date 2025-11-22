import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, Stack, TextField, Typography, Button, CircularProgress, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions } from '@mui/material';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams, Link } from 'react-router-dom';
import PreviewGallery from '../../components/previewgallery';
import { api, Cuisine, fetchPreview } from '../../lib/apiClient';
import { z } from 'zod';

const schema = z.object({
  cuisineName: z.string().min(1),
  category: z.string().optional().or(z.literal('')),
  subcategory: z.string().optional().or(z.literal('')),
  region: z.string().optional().or(z.literal('')),
  image: z.string().url('Must be a valid URL').optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function CuisineForm({ mode }: { mode: 'create' | 'edit' | 'view' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();
  const listPath = '/cuisines';

  const { data } = useQuery({
    queryKey: ['cuisine', id],
    queryFn: () => api<Cuisine>(`/cuisines/${id}`),
    enabled: mode !== 'create' && !!id,
  });

  const { register, handleSubmit, formState: { errors, isDirty }, reset, watch, setValue } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const values = watch();
  const imageValue = values.image;
  const shrinkIfFilled = (value?: string | null) => (typeof value === 'string' && value.trim().length > 0 ? { shrink: true } : undefined);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewItems, setPreviewItems] = useState<Array<{ url: string; mimeType?: string; sizeBytes?: number }>>([]);
  const [noChangesOpen, setNoChangesOpen] = useState(false);
  const [unsavedOpen, setUnsavedOpen] = useState(false);
  const [afterSaveDestination, setAfterSaveDestination] = useState<'home' | 'list'>('list');

  useEffect(() => {
    if (data) {
      reset({
        cuisineName: data.cuisineName || '',
        category: data.category || '',
        subcategory: data.subcategory || '',
        region: data.region || '',
        image: data.image || '',
      });
    }
  }, [data, reset]);

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: Cuisine = {
        cuisineName: values.cuisineName,
        category: blankToUndef(values.category),
        subcategory: blankToUndef(values.subcategory),
        region: blankToUndef(values.region),
        image: blankToUndef(values.image),
      };
      if (mode === 'create') {
        return api<Cuisine>('/cuisines', { method: 'POST', body: JSON.stringify(payload) });
      } else {
        return api<Cuisine>(`/cuisines/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cuisines'] });
      navigate(listPath);
      setAfterSaveDestination('list');
    },
  });

  const heading = mode === 'create' ? 'Create Cuisine' : mode === 'edit' ? `Edit Cuisine #${id}` : `View Cuisine #${id}`;
  const submitWithDestination = (destination: 'home' | 'list') => {
    setAfterSaveDestination(destination);
    handleSubmit((values) => {
      if (mode === 'edit' && !isDirty) {
        setNoChangesOpen(true);
        return;
      }
      mutate.mutate(values);
    })();
  };

  const handleBack = () => {
    if (mode === 'edit' && isDirty) {
      setUnsavedOpen(true);
    } else {
      navigate(listPath);
    }
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">{heading}</Typography>
        <Button variant="text" onClick={handleBack}>Back</Button>
      </Stack>
      <Card>
        <CardContent>
          <form onSubmit={(e) => { e.preventDefault(); submitWithDestination('list'); }}>
            <Stack spacing={2}>
              <TextField label="Name" {...register('cuisineName')} disabled={mode === 'view'} error={!!errors.cuisineName} helperText={errors.cuisineName?.message} InputLabelProps={shrinkIfFilled(values.cuisineName)} />
              <TextField label="Category" {...register('category')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.category)} />
              <TextField label="Subcategory" {...register('subcategory')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.subcategory)} />
              <TextField label="Region" {...register('region')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.region)} />
              <TextField label="Image URL" {...register('image')} disabled={mode === 'view'} error={!!errors.image} helperText={errors.image?.message} InputLabelProps={shrinkIfFilled(values.image)} />
              <Button
                variant="outlined"
                onClick={async () => {
                  if (!imageValue) return;
                  try {
                    const preview = await fetchPreview(imageValue);
                    setPreviewItems([{ url: preview.localUrl, mimeType: preview.mimeType, sizeBytes: preview.size }]);
                  } catch {
                    setPreviewItems([{ url: imageValue }]);
                  }
                  setPreviewOpen(true);
                }}
                disabled={!imageValue || mode === 'view'}
              >
                Preview Image
              </Button>
              <PreviewGallery
                open={previewOpen}
                onClose={() => setPreviewOpen(false)}
                items={previewItems.map(item => ({
                  ...item,
                  onDelete: () => {
                    if (mode === 'view') return;
                    setValue('image', '');
                    setPreviewOpen(false);
                    setPreviewItems([]);
                  },
                }))}
              />
              {mode !== 'view' ? (
                <Button type="submit" variant="contained" disabled={mutate.isPending} endIcon={mutate.isPending ? <CircularProgress size={16} /> : undefined}>
                  {mode === 'create' ? 'Create' : 'Save'}
                </Button>
              ) : (
                <Button variant="outlined" component={Link} to={`/cuisines/${id}`} >
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
          <Button onClick={() => navigate(listPath)}>Back to Cuisines</Button>
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

function blankToUndef<T extends string | undefined>(v: T) {
  if (!v) return undefined;
  return (v as unknown as string).trim() === '' ? undefined : v;
}
