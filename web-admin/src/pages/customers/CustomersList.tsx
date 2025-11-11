import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, CardContent, CircularProgress, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { api, Customer } from '../../lib/apiClient';

export default function CustomersList() {
  const qc = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ['customers'],
    queryFn: () => api<Customer[]>('/customers'),
  });

  const del = useMutation({
    mutationFn: (id: number) => api<void>(`/customers/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['customers'] }),
  });

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">Customers</Typography>
        <Button variant="contained" component={Link} to="/customers/new">New Customer</Button>
      </Stack>
      <Card>
        <CardContent>
          {isLoading && <CircularProgress />}
          {error && <Typography color="error">{String((error as Error).message)}</Typography>}
          {!isLoading && data && (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>First</TableCell>
                  <TableCell>Last</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Phone</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.map(c => (
                  <TableRow key={c.id}>
                    <TableCell>{c.id}</TableCell>
                    <TableCell>{c.firstName}</TableCell>
                    <TableCell>{c.lastName}</TableCell>
                    <TableCell>{c.email}</TableCell>
                    <TableCell>{c.phone}</TableCell>
                    <TableCell align="right">
                      <Button size="small" component={Link} to={`/customers/${c.id}`}>Edit</Button>
                      <Button size="small" color="error" onClick={() => c.id && del.mutate(c.id)} disabled={del.isPending}>Delete</Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </Stack>
  );
}

