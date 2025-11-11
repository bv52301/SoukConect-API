import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, CardContent, CircularProgress, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { api, Vendor } from '../../lib/apiClient';

export default function VendorsList() {
  const qc = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ['vendors'],
    queryFn: () => api<Vendor[]>('/vendors'),
  });

  const del = useMutation({
    mutationFn: (id: number) => api<void>(`/vendors/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vendors'] }),
  });

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">Vendors</Typography>
        <Button variant="contained" component={Link} to="/vendors/new">New Vendor</Button>
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
                  <TableCell>Image</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Phone</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.map(v => (
                  <TableRow key={v.vendorId}>
                    <TableCell>{v.vendorId}</TableCell>
                    <TableCell>{v.image ? <img src={v.image} alt="" style={{ maxHeight: 32, maxWidth: 48 }} onError={(ev)=>{(ev.currentTarget as HTMLImageElement).style.display='none';}} /> : '-'}</TableCell>
                    <TableCell>{v.name}</TableCell>
                    <TableCell>{v.email}</TableCell>
                    <TableCell>{v.phoneNumber}</TableCell>
                    <TableCell align="right">
                      <Button size="small" component={Link} to={`/vendors/${v.vendorId}`}>Edit</Button>
                      <Button size="small" color="error" onClick={() => v.vendorId && del.mutate(v.vendorId)} disabled={del.isPending}>Delete</Button>
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
