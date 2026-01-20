import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Link,
  ToggleButton,
  ToggleButtonGroup,
  Divider,
} from '@mui/material';

const API_BASE = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') || '';

export default function Register() {
  const navigate = useNavigate();

  // Common fields
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState<'CUSTOMER' | 'VENDOR'>('CUSTOMER');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // Customer fields
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  // Customer address fields
  const [street, setStreet] = useState('');
  const [unit, setUnit] = useState('');
  const [city, setCity] = useState('');
  const [postal, setPostal] = useState('');
  const [country, setCountry] = useState('');

  // Vendor fields
  const [businessName, setBusinessName] = useState('');
  const [businessDescription, setBusinessDescription] = useState('');
  const [contactName, setContactName] = useState('');
  const [address1, setAddress1] = useState('');
  const [address2, setAddress2] = useState('');
  const [state, setState] = useState('');
  const [landmark, setLandmark] = useState('');
  const [pincode, setPincode] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }

    // Role-specific validation
    if (role === 'CUSTOMER') {
      if (!firstName.trim()) {
        setError('First name is required');
        return;
      }
      if (!lastName.trim()) {
        setError('Last name is required');
        return;
      }
    } else if (role === 'VENDOR') {
      if (!businessName.trim()) {
        setError('Business name is required');
        return;
      }
    }

    setLoading(true);

    try {
      const payload: Record<string, unknown> = {
        email,
        password,
        role,
        phone: phone || undefined,
      };

      if (role === 'CUSTOMER') {
        payload.firstName = firstName;
        payload.lastName = lastName;
        // Customer address
        payload.street = street || undefined;
        payload.unit = unit || undefined;
        payload.city = city || undefined;
        payload.postal = postal || undefined;
        payload.country = country || undefined;
      } else if (role === 'VENDOR') {
        payload.businessName = businessName;
        payload.businessDescription = businessDescription || undefined;
        payload.contactName = contactName || undefined;
        payload.address1 = address1 || undefined;
        payload.address2 = address2 || undefined;
        payload.state = state || undefined;
        payload.landmark = landmark || undefined;
        payload.pincode = pincode || undefined;
        if (latitude) payload.latitude = parseFloat(latitude);
        if (longitude) payload.longitude = parseFloat(longitude);
      }

      const res = await fetch(`${API_BASE}/api/v1/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || 'Registration failed');
      }

      setSuccess(data.message || 'Registration successful! Please check your email to verify your account.');

      // Redirect to login after 2 seconds
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: '#f5f5f5',
        py: 4,
      }}
    >
      <Card sx={{ width: 500, maxWidth: '90%' }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h4" component="h1" gutterBottom align="center">
            Souk Connect
          </Typography>
          <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
            Create your account
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {success && (
            <Alert severity="success" sx={{ mb: 2 }}>
              {success}
            </Alert>
          )}

          <form onSubmit={handleSubmit}>
            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                I want to register as:
              </Typography>
              <ToggleButtonGroup
                value={role}
                exclusive
                onChange={(_, newRole) => newRole && setRole(newRole)}
                fullWidth
                color="primary"
              >
                <ToggleButton value="CUSTOMER">Customer</ToggleButton>
                <ToggleButton value="VENDOR">Vendor</ToggleButton>
              </ToggleButtonGroup>
            </Box>

            {/* Customer Fields */}
            {role === 'CUSTOMER' && (
              <>
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField
                    fullWidth
                    label="First Name"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    margin="normal"
                    required
                    autoFocus
                    autoComplete="given-name"
                  />
                  <TextField
                    fullWidth
                    label="Last Name"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    margin="normal"
                    required
                    autoComplete="family-name"
                  />
                </Box>

                <Divider sx={{ my: 2 }}>
                  <Typography variant="caption" color="text.secondary">
                    Address (Optional)
                  </Typography>
                </Divider>

                <TextField
                  fullWidth
                  label="Street Address"
                  value={street}
                  onChange={(e) => setStreet(e.target.value)}
                  margin="normal"
                  autoComplete="street-address"
                />
                <TextField
                  fullWidth
                  label="Unit / Apartment"
                  value={unit}
                  onChange={(e) => setUnit(e.target.value)}
                  margin="normal"
                />
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField
                    fullWidth
                    label="City"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    margin="normal"
                    autoComplete="address-level2"
                  />
                  <TextField
                    fullWidth
                    label="Postal Code"
                    value={postal}
                    onChange={(e) => setPostal(e.target.value)}
                    margin="normal"
                    autoComplete="postal-code"
                  />
                </Box>
                <TextField
                  fullWidth
                  label="Country"
                  value={country}
                  onChange={(e) => setCountry(e.target.value)}
                  margin="normal"
                  autoComplete="country-name"
                />
              </>
            )}

            {/* Vendor Fields */}
            {role === 'VENDOR' && (
              <>
                <TextField
                  fullWidth
                  label="Business Name"
                  value={businessName}
                  onChange={(e) => setBusinessName(e.target.value)}
                  margin="normal"
                  required
                  autoFocus
                  autoComplete="organization"
                />
                <TextField
                  fullWidth
                  label="Business Description"
                  value={businessDescription}
                  onChange={(e) => setBusinessDescription(e.target.value)}
                  margin="normal"
                  multiline
                  rows={2}
                />
                <TextField
                  fullWidth
                  label="Contact Name"
                  value={contactName}
                  onChange={(e) => setContactName(e.target.value)}
                  margin="normal"
                  autoComplete="name"
                />

                <Divider sx={{ my: 2 }}>
                  <Typography variant="caption" color="text.secondary">
                    Business Address
                  </Typography>
                </Divider>

                <TextField
                  fullWidth
                  label="Address Line 1"
                  value={address1}
                  onChange={(e) => setAddress1(e.target.value)}
                  margin="normal"
                  autoComplete="address-line1"
                />
                <TextField
                  fullWidth
                  label="Address Line 2"
                  value={address2}
                  onChange={(e) => setAddress2(e.target.value)}
                  margin="normal"
                  autoComplete="address-line2"
                />
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField
                    fullWidth
                    label="State"
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    margin="normal"
                    autoComplete="address-level1"
                  />
                  <TextField
                    fullWidth
                    label="Pincode"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    margin="normal"
                    autoComplete="postal-code"
                  />
                </Box>
                <TextField
                  fullWidth
                  label="Landmark"
                  value={landmark}
                  onChange={(e) => setLandmark(e.target.value)}
                  margin="normal"
                />
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField
                    fullWidth
                    label="Latitude"
                    type="number"
                    value={latitude}
                    onChange={(e) => setLatitude(e.target.value)}
                    margin="normal"
                    inputProps={{ step: 'any' }}
                  />
                  <TextField
                    fullWidth
                    label="Longitude"
                    type="number"
                    value={longitude}
                    onChange={(e) => setLongitude(e.target.value)}
                    margin="normal"
                    inputProps={{ step: 'any' }}
                  />
                </Box>
              </>
            )}

            <TextField
              fullWidth
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              margin="normal"
              required
              autoComplete="email"
            />
            <TextField
              fullWidth
              label="Phone"
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              margin="normal"
              autoComplete="tel"
            />
            <TextField
              fullWidth
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              margin="normal"
              required
              autoComplete="new-password"
              helperText="At least 8 characters"
            />
            <TextField
              fullWidth
              label="Confirm Password"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              margin="normal"
              required
              autoComplete="new-password"
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              sx={{ mt: 3 }}
            >
              {loading ? <CircularProgress size={24} color="inherit" /> : 'Register'}
            </Button>
          </form>

          <Box sx={{ mt: 2, textAlign: 'center' }}>
            <Typography variant="body2">
              Already have an account?{' '}
              <Link component={RouterLink} to="/login">
                Sign in
              </Link>
            </Typography>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
