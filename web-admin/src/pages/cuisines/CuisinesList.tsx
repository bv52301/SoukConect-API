import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, CardContent, CircularProgress, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { api, Cuisine } from '../../lib/apiClient';

export default function CuisinesList() {
  const qc = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ['cuisines'],
    queryFn: () => api<Cuisine[]>('/cuisines'),
  });

  const del = useMutation({
    mutationFn: (id: number) => api<void>(`/cuisines/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cuisines'] }),
  });

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">Cuisines</Typography>
        <Button variant="contained" component={Link} to="/cuisines/new">New Cuisine</Button>
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
                  <TableCell>Name</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Subcategory</TableCell>
                  <TableCell>Region</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.map(c => (
                  <TableRow key={c.id}>
                    <TableCell>{c.id}</TableCell>
                    <TableCell>{c.cuisineName}</TableCell>
                    <TableCell>{c.category}</TableCell>
                    <TableCell>{c.subcategory}</TableCell>
                    <TableCell>{c.region}</TableCell>
                    <TableCell align="right">
                      <Button size="small" component={Link} to={`/cuisines/${c.id}`}>Edit</Button>
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

