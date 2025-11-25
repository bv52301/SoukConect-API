import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, CardContent, CircularProgress, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography, Dialog, DialogTitle, DialogContent, DialogActions, Alert, LinearProgress } from '@mui/material';
import { Link } from 'react-router-dom';
import { api, Product } from '../../lib/apiClient';
import { useState, useRef } from 'react';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';

type BulkUpdateResult = {
  totalRows: number;
  successCount: number;
  failureCount: number;
  results: Array<{
    rowNumber: number;
    sku: string;
    success: boolean;
    message: string;
  }>;
};

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

  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [uploadResult, setUploadResult] = useState<BulkUpdateResult | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setUploadResult(null);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/products/bulk-update`, {
        method: 'POST',
        body: formData,
      });

      const result: BulkUpdateResult = await response.json();
      setUploadResult(result);

      if (result.successCount > 0) {
        qc.invalidateQueries({ queryKey: ['products'] });
      }
    } catch (err) {
      setUploadResult({
        totalRows: 0,
        successCount: 0,
        failureCount: 1,
        results: [{
          rowNumber: 0,
          sku: '',
          success: false,
          message: `Upload failed: ${err instanceof Error ? err.message : String(err)}`
        }]
      });
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const downloadTemplate = () => {
    const headers = ['SKU', 'Name', 'Price', 'Description', 'Available', 'CategoryDetails', 'Schedule', 'UseVendorSchedule', 'MediaUrls'];

    // Sample category details JSON
    const sampleCategoryDetails1 = JSON.stringify([{
      cuisineName: "Italian",
      Category: ["Pasta", "Pizza"],
      subCategories: ["Spaghetti", "Margherita"],
      regionCategory: "European"
    }]);

    const sampleCategoryDetails2 = JSON.stringify([{
      cuisineName: "Chinese",
      Category: ["Noodles", "Rice"],
      subCategories: ["Fried Rice", "Chow Mein"],
      regionCategory: "Asian"
    }]);

    // Sample schedule JSON with weekly schedules, dates, and blackout
    const sampleSchedule1 = JSON.stringify({
      weekly_schedules: [
        {
          day_of_week: ["Mon", "Tue", "Wed", "Thu", "Fri"],
          start: "09:00",
          end: "17:00",
          stock: 100,
          tz: "Asia/Singapore"
        }
      ],
      dates: [
        {
          date: "2025-12-25",
          start: "10:00",
          end: "15:00",
          stock: 50,
          tz: "Asia/Singapore"
        }
      ],
      blackout: ["2025-01-01", "2025-12-31"]
    });

    const sampleSchedule2 = JSON.stringify({
      weekly_schedules: [
        {
          day_of_week: ["Sat", "Sun"],
          start: "10:00",
          end: "22:00",
          stock: 150,
          tz: "Asia/Singapore"
        }
      ]
    });

    // Create sample rows
    const sampleRows = [
      [
        'PROD-001',
        'Sample Product 1',
        '29.99',
        'This is a sample product with full schedule',
        'true',
        sampleCategoryDetails1,
        sampleSchedule1,
        'false',
        'https://example.com/image1.jpg|https://example.com/image2.jpg'
      ],
      [
        'PROD-002',
        'Sample Product 2',
        '45.50',
        'This product uses vendor schedule',
        'true',
        sampleCategoryDetails2,
        '',
        'true',
        'https://example.com/product2.jpg'
      ],
      [
        'PROD-003',
        'Sample Product 3',
        '15.00',
        'Weekend availability only',
        'true',
        sampleCategoryDetails1,
        sampleSchedule2,
        'false',
        'https://example.com/image3a.jpg|https://example.com/image3b.jpg|https://example.com/image3c.jpg'
      ]
    ];

    // Build CSV content
    let csvContent = headers.join(',') + '\n';
    sampleRows.forEach(row => {
      // Escape fields that contain commas or quotes
      const escapedRow = row.map(field => {
        if (field.includes(',') || field.includes('"') || field.includes('\n')) {
          return `"${field.replace(/"/g, '""')}"`;
        }
        return field;
      });
      csvContent += escapedRow.join(',') + '\n';
    });

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'product-bulk-update-template.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">Products</Typography>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" startIcon={<CloudUploadIcon />} onClick={() => setUploadDialogOpen(true)}>
            Bulk Upload
          </Button>
          <Button variant="contained" component={Link} to="/products/new">New Product</Button>
        </Stack>
      </Stack>

      <Dialog open={uploadDialogOpen} onClose={() => !uploading && setUploadDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Bulk Update Products</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info">
              Upload an Excel file (.xlsx) to update multiple products at once. The file must have columns: SKU (required), Name, Price, Description, Available, CategoryDetails, Schedule, UseVendorSchedule, MediaUrls (pipe-separated URLs like "url1|url2|url3").
            </Alert>

            <Button variant="outlined" onClick={downloadTemplate}>
              Download CSV Template
            </Button>

            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.xls"
              onChange={handleFileSelect}
              style={{ display: 'none' }}
            />

            <Button
              variant="contained"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
              startIcon={uploading ? <CircularProgress size={20} /> : <CloudUploadIcon />}
            >
              {uploading ? 'Uploading...' : 'Select Excel File'}
            </Button>

            {uploading && <LinearProgress />}

            {uploadResult && (
              <Stack spacing={2}>
                <Alert severity={uploadResult.failureCount === 0 ? 'success' : uploadResult.successCount === 0 ? 'error' : 'warning'}>
                  Total: {uploadResult.totalRows} | Success: {uploadResult.successCount} | Failed: {uploadResult.failureCount}
                </Alert>

                {uploadResult.results.length > 0 && (
                  <Card variant="outlined">
                    <CardContent>
                      <Typography variant="subtitle2" gutterBottom>Results:</Typography>
                      <Stack spacing={0.5} sx={{ maxHeight: 300, overflow: 'auto' }}>
                        {uploadResult.results.map((result, idx) => (
                          <Typography
                            key={idx}
                            variant="body2"
                            color={result.success ? 'success.main' : 'error.main'}
                          >
                            Row {result.rowNumber} ({result.sku}): {result.message}
                          </Typography>
                        ))}
                      </Stack>
                    </CardContent>
                  </Card>
                )}
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)} disabled={uploading}>
            Close
          </Button>
        </DialogActions>
      </Dialog>
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
                    <Button size="small" component={Link} to={`/products/${p.id}/view`}>View</Button>
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
