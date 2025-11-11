import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, CardContent, CircularProgress, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { api, Product } from '../../lib/apiClient';

export default function ProductsList() {
  const qc = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ['products'],
    queryFn: () => api<Product[]>('/products'),
  });

  const del = useMutation({
    mutationFn: (id: number) => api<void>(`/products/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  });

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">Products</Typography>
        <Button variant="contained" component={Link} to="/products/new">New Product</Button>
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
                  <TableCell>Media</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>SKU</TableCell>
                  <TableCell>Price</TableCell>
                  <TableCell>Vendor</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.map(p => (
                  <TableRow key={p.id}>
                    <TableCell>{p.id}</TableCell>
                    <TableCell>{(p as any).media && (p as any).media.length > 0 ? (
                      ((p as any).media[0].url ? <img src={(p as any).media[0].url} style={{ maxHeight: 32, maxWidth: 48 }} onError={(ev)=>{(ev.currentTarget as HTMLImageElement).style.display='none';}} /> : '-')
                    ) : '-'}</TableCell>
                    <TableCell>{p.name}</TableCell>
                    <TableCell>{p.sku}</TableCell>
                    <TableCell>{p.price}</TableCell>
                    <TableCell>{p.vendorId}</TableCell>
                    <TableCell align="right">
                      <Button size="small" component={Link} to={`/products/${p.id}`}>Edit</Button>
                      <Button size="small" color="error" onClick={() => p.id && del.mutate(p.id)} disabled={del.isPending}>Delete</Button>
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
