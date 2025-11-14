import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
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

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <VendorsList /> },
      { path: 'vendors', element: <VendorsList /> },
      { path: 'vendors/new', element: <VendorForm mode="create" /> },
      { path: 'vendors/:id', element: <VendorForm mode="edit" /> },
      { path: 'products', element: <ProductsList /> },
      { path: 'products/new', element: <ProductForm mode="create" /> },
      { path: 'products/:id', element: <ProductForm mode="edit" /> },
      { path: 'customers', element: <CustomersList /> },
      { path: 'customers/new', element: <CustomerForm mode="create" /> },
      { path: 'customers/:id', element: <CustomerForm mode="edit" /> },
      { path: 'cuisines', element: <CuisinesList /> },
      { path: 'cuisines/new', element: <CuisineForm mode="create" /> },
      { path: 'cuisines/:id', element: <CuisineForm mode="edit" /> },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={qc}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>
);
