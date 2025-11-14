import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, Stack, TextField, Typography, Button, CircularProgress } from '@mui/material';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { api, Cuisine } from '../../lib/apiClient';
import { z } from 'zod';

const schema = z.object({
  cuisineName: z.string().min(1),
  category: z.string().optional().or(z.literal('')),
  subcategory: z.string().optional().or(z.literal('')),
  region: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function CuisineForm({ mode }: { mode: 'create' | 'edit' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();

  const { data } = useQuery({
    queryKey: ['cuisine', id],
    queryFn: () => api<Cuisine>(`/cuisines/${id}`),
    enabled: mode === 'edit' && !!id,
  });

  const { register, handleSubmit, formState: { errors }, reset } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (data) {
      reset({
        cuisineName: data.cuisineName || '',
        category: data.category || '',
        subcategory: data.subcategory || '',
        region: data.region || '',
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
      };
      if (mode === 'create') {
        return api<Cuisine>('/cuisines', { method: 'POST', body: JSON.stringify(payload) });
      } else {
        return api<Cuisine>(`/cuisines/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cuisines'] });
      navigate('/cuisines');
    },
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h5">{mode === 'create' ? 'Create Cuisine' : `Edit Cuisine #${id}`}</Typography>
      <Card>
        <CardContent>
          <form onSubmit={handleSubmit(v => mutate.mutate(v))}>
            <Stack spacing={2}>
              <TextField label="Name" {...register('cuisineName')} error={!!errors.cuisineName} helperText={errors.cuisineName?.message} />
              <TextField label="Category" {...register('category')} />
              <TextField label="Subcategory" {...register('subcategory')} />
              <TextField label="Region" {...register('region')} />
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

function blankToUndef<T extends string | undefined>(v: T) {
  if (!v) return undefined;
  return (v as unknown as string).trim() === '' ? undefined : v;
}

