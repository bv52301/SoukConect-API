import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, CardContent, CircularProgress, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography, Dialog, DialogTitle, DialogContent, DialogActions, Alert, LinearProgress } from '@mui/material';
import { Link } from 'react-router-dom';
import { api, Vendor } from '../../lib/apiClient';
import { useState, useRef } from 'react';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';

type BulkUploadResult = {
  totalRows: number;
  successCount: number;
  failureCount: number;
  results: Array<{
    rowNumber: number;
    vendorName: string;
    success: boolean;
    message: string;
    vendorId: number | null;
  }>;
};

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

  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [uploadResult, setUploadResult] = useState<BulkUploadResult | null>(null);
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

      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/vendors/bulk-upload`, {
        method: 'POST',
        body: formData,
      });

      const result: BulkUploadResult = await response.json();
      setUploadResult(result);

      if (result.successCount > 0) {
        qc.invalidateQueries({ queryKey: ['vendors'] });
      }
    } catch (err) {
      setUploadResult({
        totalRows: 0,
        successCount: 0,
        failureCount: 1,
        results: [{
          rowNumber: 0,
          vendorName: '',
          success: false,
          message: `Upload failed: ${err instanceof Error ? err.message : String(err)}`,
          vendorId: null
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
    const headers = ['Vendor Name', 'Email', 'Phone Number', 'Description', 'Address 1', 'Address 2', 'State', 'Pincode', 'Landmark', 'Contact Name', 'Latitude', 'Longitude', 'Supported Categories', 'Schedule', 'Image URL'];

    const sampleCategories = JSON.stringify(["Italian", "Chinese", "Indian"]);
    const sampleSchedule = JSON.stringify({
      weekly_schedules: [
        {
          day_of_week: ["Mon", "Tue", "Wed", "Thu", "Fri"],
          start: "09:00",
          end: "18:00",
          tz: "Asia/Singapore"
        }
      ]
    });

    const sampleRows = [
      [
        'Sample Vendor 1',
        'vendor1@example.com',
        '9876543210',
        'Fresh organic produce supplier',
        '123 Main Street',
        'Suite 100',
        'California',
        '90001',
        'Near City Hall',
        'John Doe',
        '34.052235',
        '-118.243683',
        sampleCategories,
        sampleSchedule,
        'https://example.com/vendor1.jpg'
      ],
      [
        'Sample Vendor 2',
        'vendor2@example.com',
        '9876543211',
        'Italian restaurant and catering',
        '456 Oak Avenue',
        '',
        'New York',
        '10001',
        'Next to Central Park',
        'Jane Smith',
        '40.712776',
        '-74.005974',
        JSON.stringify(["Italian", "Mediterranean"]),
        '',
        'https://example.com/vendor2.jpg'
      ]
    ];

    let csvContent = headers.join(',') + '\n';
    sampleRows.forEach(row => {
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
    a.download = 'vendor-bulk-upload-template.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">Vendors</Typography>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" startIcon={<CloudUploadIcon />} onClick={() => setUploadDialogOpen(true)}>
            Bulk Upload
          </Button>
          <Button variant="contained" component={Link} to="/vendors/new">New Vendor</Button>
        </Stack>
      </Stack>

      <Dialog open={uploadDialogOpen} onClose={() => !uploading && setUploadDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Bulk Upload Vendors</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info">
              Upload an Excel file (.xlsx) to create or update multiple vendors at once. The file must have columns: Vendor Name (required), Email, Phone Number, Description, Address fields, Latitude, Longitude, Supported Categories (JSON array), Schedule (JSON), Image URL.
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
                            Row {result.rowNumber} ({result.vendorName}): {result.message}
                            {result.vendorId && ` [ID: ${result.vendorId}]`}
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
                    <Button size="small" component={Link} to={`/vendors/${v.vendorId}/view`}>View</Button>
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
