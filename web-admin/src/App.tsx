import { AppBar, Box, Button, Container, CssBaseline, Toolbar, Typography } from '@mui/material';
import { Link, Outlet } from 'react-router-dom';

export default function App() {
  return (
    <>
      <CssBaseline />
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Souk Admin</Typography>
          <Button color="inherit" component={Link} to="/vendors">Vendors</Button>
          <Button color="inherit" component={Link} to="/products">Products</Button>
          <Button color="inherit" component={Link} to="/customers">Customers</Button>
          <Button color="inherit" component={Link} to="/cuisines">Cuisines</Button>
        </Toolbar>
      </AppBar>
      <Box sx={{ py: 3 }}>
        <Container maxWidth="lg">
          <Outlet />
        </Container>
      </Box>
    </>
  );
}
