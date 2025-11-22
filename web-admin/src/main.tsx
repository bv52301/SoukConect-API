import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import App from './App';
import VendorsList from './pages/vendors/VendorsList';
import VendorForm from './pages/vendors/VendorForm';
import ProductsList from './pages/products/ProductsList';
import ProductForm from './pages/products/ProductForm';
import CustomersList from './pages/customers/CustomersList';
import CustomerForm from './pages/customers/CustomerForm';
import CuisinesList from './pages/cuisines/CuisinesList';
import CuisineForm from './pages/cuisines/CuisineForm';

const qc = new QueryClient();

const theme = createTheme({
  components: {
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          '&.Mui-disabled .MuiOutlinedInput-notchedOutline': {
            borderColor: 'rgba(0,0,0,0.23)',
          },
          '& .MuiInputBase-input.Mui-disabled': {
            WebkitTextFillColor: 'rgba(0,0,0,0.87)',
            color: 'rgba(0,0,0,0.87)',
          },
        },
      },
    },
    MuiInputBase: {
      styleOverrides: {
        input: {
          '&.Mui-disabled': {
            WebkitTextFillColor: 'rgba(0,0,0,0.87)',
            color: 'rgba(0,0,0,0.87)',
          },
        },
      },
    },
    MuiFormLabel: {
      styleOverrides: {
        root: {
          '&.Mui-disabled': {
            color: 'rgba(0,0,0,0.87)',
          },
        },
      },
    },
  },
});

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <VendorsList /> },
      { path: 'vendors', element: <VendorsList /> },
      { path: 'vendors/new', element: <VendorForm mode="create" /> },
      { path: 'vendors/:id', element: <VendorForm mode="edit" /> },
      { path: 'vendors/:id/view', element: <VendorForm mode="view" /> },
      { path: 'products', element: <ProductsList /> },
      { path: 'products/new', element: <ProductForm mode="create" /> },
      { path: 'products/:id', element: <ProductForm mode="edit" /> },
      { path: 'products/:id/view', element: <ProductForm mode="view" /> },
      { path: 'customers', element: <CustomersList /> },
      { path: 'customers/new', element: <CustomerForm mode="create" /> },
      { path: 'customers/:id', element: <CustomerForm mode="edit" /> },
      { path: 'customers/:id/view', element: <CustomerForm mode="view" /> },
      { path: 'cuisines', element: <CuisinesList /> },
      { path: 'cuisines/new', element: <CuisineForm mode="create" /> },
      { path: 'cuisines/:id', element: <CuisineForm mode="edit" /> },
      { path: 'cuisines/:id/view', element: <CuisineForm mode="view" /> },
      { path: 'cuisines/:id/view', element: <CuisineForm mode="view" /> },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </ThemeProvider>
  </React.StrictMode>
);
