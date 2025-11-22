import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, Stack, TextField, Typography, Button, CircularProgress, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions } from '@mui/material';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { api, Customer } from '../../lib/apiClient';
import { z } from 'zod';

const schema = z.object({
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  email: z.string().email(),
  phone: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function CustomerForm({ mode }: { mode: 'create' | 'edit' | 'view' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();
  const listPath = '/customers';

  const { data } = useQuery({
    queryKey: ['customer', id],
    queryFn: () => api<Customer>(`/customers/${id}`),
    enabled: mode !== 'create' && !!id,
  });

  const { register, handleSubmit, formState: { errors, isDirty }, reset } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const [noChangesOpen, setNoChangesOpen] = useState(false);
  const [unsavedOpen, setUnsavedOpen] = useState(false);
  const [afterSaveDestination, setAfterSaveDestination] = useState<'home' | 'list'>('list');

  useEffect(() => {
    if (data) {
      reset({
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        phone: data.phone || '',
      });
    }
  }, [data, reset]);

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: Customer = {
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        phone: values.phone?.trim() || undefined,
      };
      if (mode === 'create') {
        return api<Customer>('/customers', { method: 'POST', body: JSON.stringify(payload) });
      } else {
        return api<Customer>(`/customers/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] });
      navigate(listPath);
      setAfterSaveDestination('list');
    },
  });

  const heading = mode === 'create' ? 'Create Customer' : mode === 'edit' ? `Edit Customer #${id}` : `View Customer #${id}`;
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
              <TextField label="First Name" {...register('firstName')} disabled={mode === 'view'} error={!!errors.firstName} helperText={errors.firstName?.message} />
              <TextField label="Last Name" {...register('lastName')} disabled={mode === 'view'} error={!!errors.lastName} helperText={errors.lastName?.message} />
              <TextField label="Email" {...register('email')} disabled={mode === 'view'} error={!!errors.email} helperText={errors.email?.message} />
              <TextField label="Phone" {...register('phone')} disabled={mode === 'view'} />
              {mode !== 'view' ? (
                <Button type="submit" variant="contained" disabled={mutate.isPending} endIcon={mutate.isPending ? <CircularProgress size={16} /> : undefined}>
                  {mode === 'create' ? 'Create' : 'Save'}
                </Button>
              ) : (
                <Button variant="outlined" component={Link} to={`/customers/${id}`} >
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
          <Button onClick={() => navigate(listPath)}>Back to Customers</Button>
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
