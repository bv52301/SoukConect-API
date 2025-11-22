import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, CardContent, CircularProgress, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { api, Cuisine, CuisineImage } from '../../lib/apiClient';

export default function CuisinesList() {
  const qc = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ['cuisines'],
    queryFn: () => api<Cuisine[]>('/cuisines'),
  });
  const imagesQuery = useQuery({
    queryKey: ['cuisine-images'],
    queryFn: () => api<CuisineImage[]>('/cuisines/images'),
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
          {(isLoading || imagesQuery.isLoading) && <CircularProgress />}
          {(error || imagesQuery.error) && <Typography color="error">{String((error as Error).message || (imagesQuery.error as Error)?.message)}</Typography>}
          {!isLoading && !imagesQuery.isLoading && data && imagesQuery.data && (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Cuisine</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Subcategory</TableCell>
                  <TableCell>Region</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.map(c => {
                  const cuisineImg = imagesQuery.data?.find(img => img.type === 'CUISINE' && img.name === c.cuisineName);
                  const categoryImg = imagesQuery.data?.find(img => img.type === 'CATEGORY' && img.name === c.category);
                  const subcategoryImg = imagesQuery.data?.find(img => img.type === 'SUBCATEGORY' && img.name === c.subcategory);
                  const renderThumb = (url?: string) => url ? (
                    <img
                      src={url}
                      alt=""
                      style={{ maxHeight: 40, maxWidth: 60, objectFit: 'contain' }}
                      onError={(ev) => { (ev.currentTarget as HTMLImageElement).style.display = 'none'; }}
                    />
                  ) : '-';
                  return (
                    <TableRow key={c.id}>
                      <TableCell>{c.id}</TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1} alignItems="center">
                          {renderThumb(cuisineImg?.imageUrl)}
                          <Typography variant="body2">{c.cuisineName}</Typography>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1} alignItems="center">
                          {renderThumb(categoryImg?.imageUrl)}
                          <Typography variant="body2">{c.category || '-'}</Typography>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1} alignItems="center">
                          {renderThumb(subcategoryImg?.imageUrl)}
                          <Typography variant="body2">{c.subcategory || '-'}</Typography>
                        </Stack>
                      </TableCell>
                      <TableCell>{c.region}</TableCell>
                      <TableCell align="right">
                        <Button size="small" component={Link} to={`/cuisines/${c.id}/view`}>View</Button>
                        <Button size="small" component={Link} to={`/cuisines/${c.id}`}>Edit</Button>
                          <Button size="small" color="error" onClick={() => c.id && del.mutate(c.id)} disabled={del.isPending}>Delete</Button>
                        </TableCell>
                      </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </Stack>
  );
}
