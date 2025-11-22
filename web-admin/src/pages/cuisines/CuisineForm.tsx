import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, Stack, TextField, Typography, Button, CircularProgress, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Autocomplete } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { api, Cuisine, CuisineImage } from '../../lib/apiClient';
import { z } from 'zod';

const schema = z.object({
  cuisineName: z.string().min(1),
  category: z.string().optional().or(z.literal('')),
  subcategory: z.string().optional().or(z.literal('')),
  region: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function CuisineForm({ mode }: { mode: 'create' | 'edit' | 'view' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();
  const listPath = '/cuisines';
  const disabled = mode === 'view';

  const { data } = useQuery({
    queryKey: ['cuisine', id],
    queryFn: () => api<Cuisine>(`/cuisines/${id}`),
    enabled: mode !== 'create' && !!id,
  });
  const cuisinesQuery = useQuery({
    queryKey: ['cuisines'],
    queryFn: () => api<Cuisine[]>('/cuisines'),
  });
  const imagesQuery = useQuery({
    queryKey: ['cuisine-images'],
    queryFn: () => api<CuisineImage[]>('/cuisines/images'),
  });

  const { register, handleSubmit, formState: { errors, isDirty }, reset, watch, setValue } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const values = watch();
  const shrinkIfFilled = (value?: string | null) => (typeof value === 'string' && value.trim().length > 0 ? { shrink: true } : undefined);
  const [noChangesOpen, setNoChangesOpen] = useState(false);
  const [unsavedOpen, setUnsavedOpen] = useState(false);
  const [afterSaveDestination, setAfterSaveDestination] = useState<'home' | 'list'>('list');
  const [cuisineImgUrl, setCuisineImgUrl] = useState('');
  const [categoryImgUrl, setCategoryImgUrl] = useState('');
  const [subcategoryImgUrl, setSubcategoryImgUrl] = useState('');

  useEffect(() => {
    if (data) {
      reset({
        cuisineName: data.cuisineName || '',
        category: data.category || '',
        subcategory: data.subcategory || '',
        region: data.region || '',
      });
      setCuisineImgUrl('');
      setCategoryImgUrl('');
      setSubcategoryImgUrl('');
    }
  }, [data, reset]);

  const currentImages = useMemo(() => {
    const list = imagesQuery.data || [];
    const get = (type: CuisineImage['type'], name?: string | null) =>
      list.find(img => img.type === type && name && img.name === name);
    return {
      cuisine: get('CUISINE', values.cuisineName),
      category: get('CATEGORY', values.category),
      subcategory: get('SUBCATEGORY', values.subcategory),
    };
  }, [imagesQuery.data, values.cuisineName, values.category, values.subcategory]);

  useEffect(() => {
    if (currentImages.cuisine?.imageUrl) setCuisineImgUrl(currentImages.cuisine.imageUrl);
    if (currentImages.category?.imageUrl) setCategoryImgUrl(currentImages.category.imageUrl);
    if (currentImages.subcategory?.imageUrl) setSubcategoryImgUrl(currentImages.subcategory.imageUrl);
  }, [currentImages.cuisine?.imageUrl, currentImages.category?.imageUrl, currentImages.subcategory?.imageUrl]);

  const saveImage = async (existing: CuisineImage | undefined, type: CuisineImage['type'], name: string | null | undefined, url: string) => {
    if (!name) return;
    const body = { type, name, imageUrl: url };
    if (existing?.id) {
      if (!window.confirm(`You are changing the image for all ${name}. Continue?`)) return;
      await api<CuisineImage>(`/cuisines/images/${existing.id}`, { method: 'PUT', body: JSON.stringify(body) });
    } else {
      await api<CuisineImage>(`/cuisines/images`, { method: 'POST', body: JSON.stringify(body) });
    }
    qc.invalidateQueries({ queryKey: ['cuisine-images'] });
  };

  const deleteImage = async (existing?: CuisineImage) => {
    if (!existing?.id) return;
    await api<void>(`/cuisines/images/${existing.id}`, { method: 'DELETE' });
    qc.invalidateQueries({ queryKey: ['cuisine-images'] });
  };

  const cuisineOptions = useMemo(() => Array.from(new Set((cuisinesQuery.data || []).map(c => c.cuisineName).filter(Boolean))), [cuisinesQuery.data]);
  const categoryOptions = useMemo(() => {
    const all = cuisinesQuery.data || [];
    if (values.cuisineName) {
      return Array.from(new Set(all.filter(c => c.cuisineName === values.cuisineName).map(c => c.category).filter(Boolean)));
    }
    return Array.from(new Set(all.map(c => c.category).filter(Boolean)));
  }, [cuisinesQuery.data, values.cuisineName]);
  const subcategoryOptions = useMemo(() => {
    const all = cuisinesQuery.data || [];
    if (values.cuisineName && values.category) {
      return Array.from(new Set(all.filter(c => c.cuisineName === values.cuisineName && c.category === values.category).map(c => c.subcategory).filter(Boolean)));
    }
    if (values.category) {
      return Array.from(new Set(all.filter(c => c.category === values.category).map(c => c.subcategory).filter(Boolean)));
    }
    return Array.from(new Set(all.map(c => c.subcategory).filter(Boolean)));
  }, [cuisinesQuery.data, values.cuisineName, values.category]);

  const applyAutoFillFromCuisine = (name: string) => {
    const match = (cuisinesQuery.data || []).find(c => c.cuisineName === name);
    if (match) {
      setValue('category', match.category || '', { shouldDirty: true });
      setValue('subcategory', match.subcategory || '', { shouldDirty: true });
    }
  };

  const applyAutoFillFromCategory = (cat: string) => {
    const match = (cuisinesQuery.data || []).find(c => c.category === cat && (!values.cuisineName || c.cuisineName === values.cuisineName));
    if (match) {
      setValue('subcategory', match.subcategory || '', { shouldDirty: true });
    }
  };

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: Cuisine = {
        cuisineName: values.cuisineName,
        category: blankToUndef(values.category),
        subcategory: blankToUndef(values.subcategory),
        region: blankToUndef(values.region),
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
              <Autocomplete
                freeSolo
                options={cuisineOptions}
                value={values.cuisineName || ''}
    onChange={(_, v) => { setValue('cuisineName', v || '', { shouldDirty: true }); applyAutoFillFromCuisine(v || ''); }}
    onInputChange={(_, v, reason) => { if (reason === 'input') setValue('cuisineName', v, { shouldDirty: true }); }}
                renderInput={(params) => (
                  <TextField {...params} label="Name" disabled={disabled} error={!!errors.cuisineName} helperText={errors.cuisineName?.message} InputLabelProps={shrinkIfFilled(values.cuisineName)} />
                )}
              />
              <Stack spacing={1} sx={{ pl: 1 }}>
                <Typography variant="caption">Cuisine Image</Typography>
                {currentImages.cuisine?.imageUrl ? (
                  <img src={currentImages.cuisine.imageUrl} alt="" style={{ maxHeight: 80, maxWidth: 120, objectFit: 'contain' }} onError={(ev)=>{(ev.currentTarget as HTMLImageElement).style.display='none';}} />
                ) : <Typography variant="caption">No image</Typography>}
                {!disabled && (
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                    <TextField size="small" label="URL" value={cuisineImgUrl} onChange={e=>setCuisineImgUrl(e.target.value)} fullWidth />
                    <Button variant="outlined" size="small" onClick={()=> saveImage(currentImages.cuisine, 'CUISINE', values.cuisineName, cuisineImgUrl)} disabled={!cuisineImgUrl.trim() || !values.cuisineName}>Save</Button>
                    {currentImages.cuisine?.id ? <Button variant="text" size="small" color="error" onClick={()=> deleteImage(currentImages.cuisine)}>Delete</Button> : null}
                  </Stack>
                )}
              </Stack>
              <Autocomplete
                freeSolo
                options={categoryOptions}
                value={values.category || ''}
    onChange={(_, v) => { setValue('category', v || '', { shouldDirty: true }); applyAutoFillFromCategory(v || ''); }}
    onInputChange={(_, v, reason) => { if (reason === 'input') setValue('category', v, { shouldDirty: true }); }}
                renderInput={(params) => (
                  <TextField {...params} label="Category" disabled={disabled} InputLabelProps={shrinkIfFilled(values.category)} />
                )}
              />
              <Stack spacing={1} sx={{ pl: 1 }}>
                <Typography variant="caption">Category Image</Typography>
                {currentImages.category?.imageUrl ? (
                  <img src={currentImages.category.imageUrl} alt="" style={{ maxHeight: 80, maxWidth: 120, objectFit: 'contain' }} onError={(ev)=>{(ev.currentTarget as HTMLImageElement).style.display='none';}} />
                ) : <Typography variant="caption">No image</Typography>}
                {!disabled && (
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                    <TextField size="small" label="URL" value={categoryImgUrl} onChange={e=>setCategoryImgUrl(e.target.value)} fullWidth />
                    <Button variant="outlined" size="small" onClick={()=> saveImage(currentImages.category, 'CATEGORY', values.category, categoryImgUrl)} disabled={!categoryImgUrl.trim() || !values.category}>Save</Button>
                    {currentImages.category?.id ? <Button variant="text" size="small" color="error" onClick={()=> deleteImage(currentImages.category)}>Delete</Button> : null}
                  </Stack>
                )}
              </Stack>
              <Autocomplete
                freeSolo
                options={subcategoryOptions}
                value={values.subcategory || ''}
    onChange={(_, v) => setValue('subcategory', v || '', { shouldDirty: true })}
    onInputChange={(_, v, reason) => { if (reason === 'input') setValue('subcategory', v, { shouldDirty: true }); }}
                renderInput={(params) => (
                  <TextField {...params} label="Subcategory" disabled={disabled} InputLabelProps={shrinkIfFilled(values.subcategory)} />
                )}
              />
              <Stack spacing={1} sx={{ pl: 1 }}>
                <Typography variant="caption">Subcategory Image</Typography>
                {currentImages.subcategory?.imageUrl ? (
                  <img src={currentImages.subcategory.imageUrl} alt="" style={{ maxHeight: 80, maxWidth: 120, objectFit: 'contain' }} onError={(ev)=>{(ev.currentTarget as HTMLImageElement).style.display='none';}} />
                ) : <Typography variant="caption">No image</Typography>}
                {!disabled && (
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                    <TextField size="small" label="URL" value={subcategoryImgUrl} onChange={e=>setSubcategoryImgUrl(e.target.value)} fullWidth />
                    <Button variant="outlined" size="small" onClick={()=> saveImage(currentImages.subcategory, 'SUBCATEGORY', values.subcategory, subcategoryImgUrl)} disabled={!subcategoryImgUrl.trim() || !values.subcategory}>Save</Button>
                    {currentImages.subcategory?.id ? <Button variant="text" size="small" color="error" onClick={()=> deleteImage(currentImages.subcategory)}>Delete</Button> : null}
                  </Stack>
                )}
              </Stack>
              <TextField label="Region" {...register('region')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.region)} />
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
