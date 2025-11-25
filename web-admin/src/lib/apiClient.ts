const base = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') || '';

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${base}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
    ...init,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status}: ${text}`);
  }
  // Some DELETE endpoints return empty body
  if (res.status === 204) return undefined as unknown as T;
  return (await res.json()) as T;
}

export async function fetchPreview(url: string): Promise<{ localUrl: string; mimeType?: string; size?: number }>
{
  return api('/preview/fetch', { method: 'POST', body: JSON.stringify({ url }) });
}

export type Vendor = {
  vendorId?: number;
  name: string;
  description?: string;
  supportedCategories?: unknown;
  schedule?: unknown;
  image?: string;
  address1?: string;
  address2?: string;
  state?: string;
  landmark?: string;
  pincode?: string;
  contactName?: string;
  phoneNumber?: string;
  email?: string;
  latitude?: number;
  longitude?: number;
  createdAt?: string;
  scheduleUpdated?: string;
};

export type VendorLocation = {
  id?: number;
  vendorId: number;
  locationName: string;
  latitude: number;
  longitude: number;
  address1?: string;
  address2?: string;
  state?: string;
  pincode?: string;
  landmark?: string;
  schedule?: unknown;
  status?: 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
  updatedAt?: string;
};

export type ProductLocationAssignment = {
  vendorLocationId: number;
  locationName?: string;
  sku: string;
  available?: boolean;
  stock?: number;
};

export type Product = {
  id?: number;
  name: string;
  sku: string;
  price: number;
  vendorId: number;
  description?: string;
  available?: boolean;
  categories?: unknown[];
  categoryDetails?: unknown;
  schedule?: unknown;
  useVendorSchedule?: boolean;
  locations?: ProductLocationAssignment[];
  media?: Array<{ id?: number; url: string; mediaType?: string; description?: string }>; // optional from API
};

export type Customer = {
  id?: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
};

export type Cuisine = {
  id?: number;
  cuisineName: string;
  category?: string;
  subcategory?: string;
  region?: string;
  image?: string;
};

export type CuisineImage = {
  id?: number;
  type: 'CUISINE' | 'CATEGORY' | 'SUBCATEGORY';
  name: string;
  imageUrl: string;
  description?: string;
};
