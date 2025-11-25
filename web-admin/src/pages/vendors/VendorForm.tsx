import { zodResolver } from '@hookform/resolvers/zod';
import { Card, CardContent, Stack, TextField, Typography, Button, CircularProgress, Autocomplete, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Divider, IconButton } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import PreviewGallery from '../../components/previewgallery';
import { fetchPreview } from '../../lib/apiClient';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { api, Vendor, VendorLocation } from '../../lib/apiClient';
import { z } from 'zod';

const schema = z.object({
  name: z.string().min(1),
  description: z.string().max(1000, 'Max 1000 characters').optional().or(z.literal('')),
  email: z.string().email().optional().or(z.literal('')),
  phoneNumber: z.string().optional().or(z.literal('')),
  address1: z.string().optional().or(z.literal('')),
  address2: z.string().optional().or(z.literal('')),
  state: z.string().optional().or(z.literal('')),
  pincode: z.string().optional().or(z.literal('')),
  contactName: z.string().optional().or(z.literal('')),
  image: z.string().url().optional().or(z.literal('')),
  latitude: z.string().optional().or(z.literal('')),
  longitude: z.string().optional().or(z.literal('')),
  schedule: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

export default function VendorForm({ mode }: { mode: 'create' | 'edit' | 'view' }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const qc = useQueryClient();
  const listPath = '/vendors';

  const { data } = useQuery({
    queryKey: ['vendor', id],
    queryFn: () => api<Vendor>(`/vendors/${id}`),
    enabled: mode !== 'create' && !!id,
  });

  // Vendor locations query
  const locationsQuery = useQuery({
    queryKey: ['vendor-locations', id],
    queryFn: () => api<VendorLocation[]>(`/vendors/${id}/locations`),
    enabled: mode !== 'create' && !!id,
  });

  const { register, handleSubmit, formState: { errors, isDirty }, reset, watch, setValue } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const values = watch();
  const imageValue = values.image;
  const shrinkIfFilled = (value?: string | null) => (typeof value === 'string' && value.trim().length > 0 ? { shrink: true } : undefined);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [items, setItems] = useState<Array<{url:string; mimeType?: string; sizeBytes?: number}>>([]);
  const [supportedCats, setSupportedCats] = useState<string[]>([]);
  const [initialSupportedCats, setInitialSupportedCats] = useState<string[]>([]);
  const [noChangesOpen, setNoChangesOpen] = useState(false);
  const [unsavedOpen, setUnsavedOpen] = useState(false);
  const [afterSaveDestination, setAfterSaveDestination] = useState<'home' | 'list'>('list');

  // Schedule state
  type WeeklyItem = { day_of_week: string[]; start: string; end: string; tz: string };
  type DateItem = { date: string; start: string; end: string; tz: string };
  const [weekly, setWeekly] = useState<WeeklyItem[]>([]);
  const [datesArr, setDatesArr] = useState<DateItem[]>([]);
  const [blackoutArr, setBlackoutArr] = useState<string[]>([]);
  const dowOptions = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  const scheduleSyncInitialized = useRef(false);

  // Additional locations state
  type LocationEntry = {
    id?: number;
    locationName: string;
    latitude: string;
    longitude: string;
    address1: string;
    address2: string;
    state: string;
    pincode: string;
    landmark: string;
    status: 'ACTIVE' | 'INACTIVE';
  };
  const [locationEntries, setLocationEntries] = useState<LocationEntry[]>([]);
  const [deletedLocationIds, setDeletedLocationIds] = useState<number[]>([]);

  // Distinct categories from cuisines service
  const catsQuery = useQuery({
    queryKey: ['cuisine-categories'],
    queryFn: async () => {
      const res = await api<Array<{ category: string; imageUrl?: string | null }>>('/cuisines/categories');
      return res.map(r => r.category).filter((s): s is string => !!s);
    },
  });

  useEffect(() => {
    if (data) {
      reset({
        name: data.name || '',
        description: data.description || '',
        email: data.email || '',
        phoneNumber: data.phoneNumber || '',
        address1: data.address1 || '',
        address2: data.address2 || '',
        state: data.state || '',
        pincode: data.pincode || '',
        contactName: data.contactName || '',
        image: data.image || '',
        latitude: data.latitude ? String(data.latitude) : '',
        longitude: data.longitude ? String(data.longitude) : '',
        schedule: data.schedule ? JSON.stringify(data.schedule, null, 2) : '',
      });
      // Hydrate supported categories from array or map
      const sc: any = data.supportedCategories;
      if (Array.isArray(sc)) {
        const hydrated = sc.filter((s:any)=> typeof s === 'string');
        setSupportedCats(hydrated);
        setInitialSupportedCats(hydrated);
      }
      else if (sc && typeof sc === 'object') {
        const hydrated = Object.keys(sc);
        setSupportedCats(hydrated);
        setInitialSupportedCats(hydrated);
      } else {
        setSupportedCats([]);
        setInitialSupportedCats([]);
      }
      // Pre-populate schedules
      try {
        const schedData: any = data.schedule || undefined;
        if (schedData) {
          if (Array.isArray(schedData.weekly_schedules)) {
            setWeekly(schedData.weekly_schedules.map((w:any) => ({
              day_of_week: Array.isArray(w.day_of_week) ? w.day_of_week : [],
              start: w.start || '', end: w.end || '', tz: w.tz || 'Asia/Singapore'
            })));
          }
          if (Array.isArray(schedData.dates)) {
            setDatesArr(schedData.dates.map((d:any) => ({ date: d.date || '', start: d.start || '', end: d.end || '', tz: d.tz || 'Asia/Singapore' })));
          }
          if (Array.isArray(schedData.blackout)) {
            setBlackoutArr(schedData.blackout.filter((x:any)=>!!x));
          }
        }
      } catch {}
      scheduleSyncInitialized.current = false;
    } else {
      setInitialSupportedCats([]);
    }
  }, [data, reset]);

  // Populate location entries from query
  useEffect(() => {
    if (locationsQuery.data) {
      setLocationEntries(locationsQuery.data.map(loc => ({
        id: loc.id,
        locationName: loc.locationName,
        latitude: loc.latitude ? String(loc.latitude) : '',
        longitude: loc.longitude ? String(loc.longitude) : '',
        address1: loc.address1 || '',
        address2: loc.address2 || '',
        state: loc.state || '',
        pincode: loc.pincode || '',
        landmark: loc.landmark || '',
        status: loc.status || 'ACTIVE'
      })));
    }
  }, [locationsQuery.data]);

  // Auto-update schedule JSON from UI rows
  useEffect(() => {
    const sc = buildSchedule(weekly, datesArr, blackoutArr);
    if (sc) setValue('schedule', JSON.stringify(sc, null, 2), { shouldDirty: scheduleSyncInitialized.current, shouldValidate: false });
    else setValue('schedule', '', { shouldDirty: scheduleSyncInitialized.current, shouldValidate: false });
    scheduleSyncInitialized.current = true;
  }, [weekly, datesArr, blackoutArr, setValue]);

  const mutate = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: Vendor = {
        name: values.name,
        description: blankToUndef(values.description),
        email: blankToUndef(values.email),
        phoneNumber: blankToUndef(values.phoneNumber),
        address1: blankToUndef(values.address1),
        address2: blankToUndef(values.address2),
        state: blankToUndef(values.state),
        pincode: blankToUndef(values.pincode),
        contactName: blankToUndef(values.contactName),
        image: blankToUndef(values.image),
        latitude: values.latitude ? parseFloat(values.latitude) : undefined,
        longitude: values.longitude ? parseFloat(values.longitude) : undefined,
        supportedCategories: supportedCats.length ? supportedCats : undefined,
        schedule: parseJsonSafe(values.schedule),
      };
      if (mode === 'create') {
        return api<Vendor>('/vendors', { method: 'POST', body: JSON.stringify(payload) });
      } else {
        return api<Vendor>(`/vendors/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
      }
    },
    onSuccess: async (savedVendor) => {
      // Save/update/delete locations for edit mode
      if (mode === 'edit' && id) {
        // Delete removed locations
        for (const locId of deletedLocationIds) {
          await api(`/vendors/${id}/locations/${locId}`, { method: 'DELETE' });
        }

        // Create or update locations
        for (const loc of locationEntries) {
          const locationPayload = {
            locationName: loc.locationName,
            latitude: loc.latitude ? parseFloat(loc.latitude) : 0,
            longitude: loc.longitude ? parseFloat(loc.longitude) : 0,
            address1: blankToUndef(loc.address1),
            address2: blankToUndef(loc.address2),
            state: blankToUndef(loc.state),
            pincode: blankToUndef(loc.pincode),
            landmark: blankToUndef(loc.landmark),
            status: loc.status
          };

          if (loc.id) {
            // Update existing
            await api(`/vendors/${id}/locations/${loc.id}`, {
              method: 'PUT',
              body: JSON.stringify(locationPayload)
            });
          } else if (loc.locationName.trim()) {
            // Create new (only if name is provided)
            await api(`/vendors/${id}/locations`, {
              method: 'POST',
              body: JSON.stringify(locationPayload)
            });
          }
        }
        qc.invalidateQueries({ queryKey: ['vendor-locations', id] });
      }

      qc.invalidateQueries({ queryKey: ['vendors'] });
      qc.invalidateQueries({ queryKey: ['vendor-locations'] }); // Invalidate global locations query
      navigate(listPath);
      setAfterSaveDestination('list');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async () => api<void>(`/vendors/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vendors'] });
      navigate('/vendors');
    },
  });

  const catsChanged = JSON.stringify([...supportedCats].sort()) !== JSON.stringify([...initialSupportedCats].sort());
  const hasUnsavedChanges = isDirty || catsChanged;

  const submitWithDestination = (destination: 'home' | 'list') => {
    setAfterSaveDestination(destination);
    handleSubmit((values) => {
      if (mode === 'edit' && !hasUnsavedChanges) {
        setNoChangesOpen(true);
        return;
      }
      mutate.mutate(values);
    })();
  };

  const handleBack = () => {
    if (mode === 'edit' && hasUnsavedChanges) {
      setUnsavedOpen(true);
    } else {
      navigate(listPath);
    }
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5">{mode === 'create' ? 'Create Vendor' : `Edit Vendor #${id}`}</Typography>
        <Button variant="text" onClick={handleBack}>Back</Button>
      </Stack>
      <Card>
        <CardContent>
          <form onSubmit={(e) => { e.preventDefault(); submitWithDestination('list'); }}>
            <Stack spacing={2}>
              <TextField label="Name" {...register('name')} disabled={mode === 'view'} error={!!errors.name} helperText={errors.name?.message} InputLabelProps={shrinkIfFilled(values.name)} />
              <TextField label="Description" {...register('description')} disabled={mode === 'view'} multiline minRows={3} inputProps={{ maxLength: 1000 }} InputLabelProps={shrinkIfFilled(values.description)} />
              <TextField label="Email" {...register('email')} disabled={mode === 'view'} error={!!errors.email} helperText={errors.email?.message} InputLabelProps={shrinkIfFilled(values.email)} />
              <TextField label="Phone" {...register('phoneNumber')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.phoneNumber)} />
              <TextField label="Address 1" {...register('address1')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.address1)} />
              <TextField label="Address 2" {...register('address2')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.address2)} />
              <TextField label="State" {...register('state')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.state)} />
              <TextField label="Pincode" {...register('pincode')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.pincode)} />
              <TextField label="Contact Name" {...register('contactName')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.contactName)} />
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label="Latitude"
                  {...register('latitude')}
                  disabled={mode === 'view'}
                  type="number"
                  inputProps={{ step: 'any' }}
                  placeholder="e.g., 1.290270"
                  InputLabelProps={shrinkIfFilled(values.latitude)}
                  fullWidth
                />
                <TextField
                  label="Longitude"
                  {...register('longitude')}
                  disabled={mode === 'view'}
                  type="number"
                  inputProps={{ step: 'any' }}
                  placeholder="e.g., 103.851959"
                  InputLabelProps={shrinkIfFilled(values.longitude)}
                  fullWidth
                />
              </Stack>
              <TextField label="Image URL" {...register('image')} disabled={mode === 'view'} InputLabelProps={shrinkIfFilled(values.image)} />
              {imageValue ? (
                <Card variant="outlined">
                  <CardContent sx={{ display: 'flex', justifyContent: 'center' }}>
                    <img
                      src={imageValue}
                      alt="Vendor"
                      style={{ maxHeight: 180, maxWidth: '100%', objectFit: 'contain' }}
                      onError={(ev) => { (ev.currentTarget as HTMLImageElement).style.display = 'none'; }}
                    />
                  </CardContent>
                </Card>
              ) : null}
              {mode === 'edit' && imageValue ? (
                <Button
                  color="error"
                  variant="outlined"
                  onClick={() => {
                    setValue('image', '', { shouldDirty: true });
                    setItems([]);
                  }}
                >
                  Delete Image
                </Button>
              ) : null}
              <Button
                variant="outlined"
                onClick={async () => {
                  if (!imageValue) return;
                  try {
                    const p = await fetchPreview(imageValue);
                    setItems([{ url: p.localUrl, mimeType: p.mimeType, sizeBytes: p.size }]);
                    setPreviewOpen(true);
                  } catch {
                    setItems([{ url: imageValue }]);
                    setPreviewOpen(true);
                  }
                }}
                disabled={!imageValue || mode === 'view'}
              >Preview</Button>
              <PreviewGallery open={previewOpen} onClose={() => setPreviewOpen(false)} items={items.map(it => ({...it, onDelete: () => { setValue('image',''); setPreviewOpen(false); setItems([]); }}))} />
              <Autocomplete
                multiple
                options={catsQuery.data ?? []}
                value={supportedCats}
                onChange={(_, v) => setSupportedCats(v)}
                renderInput={(params) => <TextField {...params} label="Supported Categories" placeholder="Select categories" />}
                disabled={mode === 'view'}
              />
              <Divider />
              <Typography variant="subtitle1">Schedules</Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" variant="outlined" onClick={() => setWeekly(w => [...w, { day_of_week: [], start: '', end: '', tz: 'Asia/Singapore' }])} disabled={mode === 'view'}>+ Weekly</Button>
                <Button size="small" variant="outlined" onClick={() => setDatesArr(d => [...d, { date: '', start: '', end: '', tz: 'Asia/Singapore' }])} disabled={mode === 'view'}>+ Date</Button>
                <Button size="small" variant="outlined" onClick={() => setBlackoutArr(b => [...b, ''])} disabled={mode === 'view'}>+ Blackout</Button>
              </Stack>

              {/* Weekly rows */}
              {weekly.map((w, idx) => (
                <Stack key={`w-${idx}`} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
                  <Autocomplete<string, true, false, false>
                    multiple
                    sx={{ minWidth: 220, flex:1 }}
                    options={dowOptions}
                    value={w.day_of_week}
                    onChange={(_, v) => setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, day_of_week: v } : it))}
                    renderInput={(p)=> <TextField {...p} label="Days of week" />}
                  />
                  <TextField sx={{ width: 130 }} type="time" label="Start" value={w.start} onChange={e=> setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, start: e.target.value } : it))} />
                  <TextField sx={{ width: 130 }} type="time" label="End" value={w.end} onChange={e=> setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, end: e.target.value } : it))} />
                  <TextField sx={{ minWidth: 180 }} label="Timezone" value={w.tz} onChange={e=> setWeekly(arr => arr.map((it,i)=> i===idx ? { ...it, tz: e.target.value } : it))} />
                  <IconButton aria-label="remove" onClick={()=> setWeekly(arr => arr.filter((_,i)=> i!==idx))} disabled={mode === 'view'}><DeleteIcon /></IconButton>
                </Stack>
              ))}

              {/* Date rows */}
              {datesArr.map((d, idx) => (
                <Stack key={`d-${idx}`} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
                  <TextField sx={{ width: 170 }} type="date" label="Date" value={d.date} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, date: e.target.value } : it))} />
                  <TextField sx={{ width: 130 }} type="time" label="Start" value={d.start} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, start: e.target.value } : it))} />
                  <TextField sx={{ width: 130 }} type="time" label="End" value={d.end} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, end: e.target.value } : it))} />
                  <TextField sx={{ minWidth: 180 }} label="Timezone" value={d.tz} onChange={e=> setDatesArr(arr => arr.map((it,i)=> i===idx ? { ...it, tz: e.target.value } : it))} />
                  <IconButton aria-label="remove" onClick={()=> setDatesArr(arr => arr.filter((_,i)=> i!==idx))} disabled={mode === 'view'}><DeleteIcon /></IconButton>
                </Stack>
              ))}

              {/* Blackout dates */}
              {blackoutArr.map((b, idx) => (
                <Stack key={`b-${idx}`} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems="center">
                  <TextField sx={{ width: 170 }} type="date" label="Blackout date" value={b} onChange={e=> setBlackoutArr(arr => arr.map((it,i)=> i===idx ? e.target.value : it))} />
                  <IconButton aria-label="remove" onClick={()=> setBlackoutArr(arr => arr.filter((_,i)=> i!==idx))} disabled={mode === 'view'}><DeleteIcon /></IconButton>
                </Stack>
              ))}
              <TextField label="schedule (JSON)" {...register('schedule')} multiline minRows={4} InputLabelProps={shrinkIfFilled(values.schedule)} />

              {mode === 'edit' && (
                <>
                  <Divider />
                  <Typography variant="subtitle1">Additional Locations (Branches)</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Manage multiple physical locations for this vendor. The primary location is set above.
                  </Typography>
                  <Stack spacing={2}>
                    {locationEntries.map((loc, idx) => (
                      <Card key={`loc-${idx}`} variant="outlined">
                        <CardContent>
                          <Stack spacing={2}>
                            <Stack direction="row" justifyContent="space-between" alignItems="center">
                              <Typography variant="subtitle2">
                                Location {idx + 1} {loc.id ? `(ID: ${loc.id})` : '(New)'}
                              </Typography>
                              <IconButton
                                aria-label="remove"
                                color="error"
                                onClick={() => {
                                  if (loc.id) {
                                    setDeletedLocationIds(prev => [...prev, loc.id!]);
                                  }
                                  setLocationEntries(arr => arr.filter((_, i) => i !== idx));
                                }}
                              >
                                <DeleteIcon />
                              </IconButton>
                            </Stack>
                            <TextField
                              label="Location Name"
                              value={loc.locationName}
                              onChange={e => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, locationName: e.target.value } : it))}
                              placeholder="e.g., Downtown Branch"
                              required
                            />
                            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                              <TextField
                                label="Latitude"
                                value={loc.latitude}
                                onChange={e => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, latitude: e.target.value } : it))}
                                type="number"
                                slotProps={{ htmlInput: { step: 'any' } }}
                                placeholder="e.g., 1.290270"
                                fullWidth
                              />
                              <TextField
                                label="Longitude"
                                value={loc.longitude}
                                onChange={e => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, longitude: e.target.value } : it))}
                                type="number"
                                slotProps={{ htmlInput: { step: 'any' } }}
                                placeholder="e.g., 103.851959"
                                fullWidth
                              />
                            </Stack>
                            <TextField
                              label="Address 1"
                              value={loc.address1}
                              onChange={e => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, address1: e.target.value } : it))}
                            />
                            <TextField
                              label="Address 2"
                              value={loc.address2}
                              onChange={e => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, address2: e.target.value } : it))}
                            />
                            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                              <TextField
                                label="State"
                                value={loc.state}
                                onChange={e => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, state: e.target.value } : it))}
                                fullWidth
                              />
                              <TextField
                                label="Pincode"
                                value={loc.pincode}
                                onChange={e => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, pincode: e.target.value } : it))}
                                fullWidth
                              />
                            </Stack>
                            <TextField
                              label="Landmark"
                              value={loc.landmark}
                              onChange={e => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, landmark: e.target.value } : it))}
                            />
                            <Autocomplete
                              options={['ACTIVE', 'INACTIVE'] as const}
                              value={loc.status}
                              onChange={(_, val) => setLocationEntries(arr => arr.map((it, i) => i === idx ? { ...it, status: val || 'ACTIVE' } : it))}
                              renderInput={(params) => <TextField {...params} label="Status" />}
                            />
                          </Stack>
                        </CardContent>
                      </Card>
                    ))}
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => setLocationEntries(arr => [...arr, {
                        locationName: '',
                        latitude: '',
                        longitude: '',
                        address1: '',
                        address2: '',
                        state: '',
                        pincode: '',
                        landmark: '',
                        status: 'ACTIVE'
                      }])}
                    >
                      + Add Location
                    </Button>
                  </Stack>
                </>
              )}

              <Divider />
              {mode !== 'view' ? (
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                  <Button type="submit" variant="contained" disabled={mutate.isPending || deleteMutation.isPending} endIcon={mutate.isPending ? <CircularProgress size={16} /> : undefined}>
                    {mode === 'create' ? 'Create' : 'Save'}
                  </Button>
                  {mode === 'edit' && id ? (
                    <Button color="error" variant="outlined" disabled={deleteMutation.isPending || mutate.isPending} onClick={() => { if (window.confirm('Delete this vendor?')) deleteMutation.mutate(); }}>
                      Delete
                    </Button>
                  ) : null}
                </Stack>
              ) : (
                <Button variant="outlined" component={Link} to={`/vendors/${id}`} >
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
          <Button onClick={() => navigate(listPath)}>Back to Vendors</Button>
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

function parseJsonSafe(s?: string) {
  if (!s) return undefined;
  try { return JSON.parse(s); } catch { return undefined; }
}

function blankToUndef<T extends string | undefined>(v: T) {
  if (!v) return undefined;
  return (v as unknown as string).trim() === '' ? undefined : v;
}

function buildSchedule(
  weekly: { day_of_week: string[]; start: string; end: string; tz: string }[],
  datesArr: { date: string; start: string; end: string; tz: string }[],
  blackoutArr: string[]
) {
  const sc: any = {};
  const w = weekly.filter(x => x.day_of_week.length && x.start && x.end).map(x => ({
    day_of_week: x.day_of_week,
    start: x.start,
    end: x.end,
    tz: x.tz || 'Asia/Singapore'
  }));
  if (w.length) sc.weekly_schedules = w;
  const d = datesArr.filter(x => x.date && x.start && x.end).map(x => ({
    date: x.date,
    start: x.start,
    end: x.end,
    tz: x.tz || 'Asia/Singapore'
  }));
  if (d.length) sc.dates = d;
  const b = blackoutArr.filter(Boolean);
  if (b.length) sc.blackout = b;
  return Object.keys(sc).length ? sc : undefined;
}
