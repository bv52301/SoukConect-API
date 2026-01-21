/* import type { Chef } from './types'; */
import type { Chef } from '@soukconect/types';

export async function getTopChefs(): Promise<Chef[]> {
  // Simulate API delay
  await new Promise(resolve => setTimeout(resolve, 100));

  return [
    {
      id: '1',
      name: 'Maria Santos',
      rating: 4.9,
      specialty: 'Artisan Sweets & Baked Treasures',
      totalOrders: 2847,
      products: 15,
      experience: 8,
      tags: ['Sweet', 'Cakes', 'Cookies'],
      bio: 'Passionate baker with 8 years of experience creating artisanal sweets and baked goods using traditional recipes passed down through generations.',
    },
    {
      id: '2',
      name: 'Giuseppe Romano',
      rating: 5.0,
      specialty: 'Authentic Italian Pasta Art & Foods',
      totalOrders: 2156,
      products: 12,
      experience: 15,
      tags: ['Pasta', 'Italian', 'Fresh'],
      bio: 'Master pasta maker from Bologna, bringing authentic Italian flavors and traditional pasta-making techniques to your table.',
    },
    {
      id: '3',
      name: 'Emma Thompson',
      rating: 4.8,
      specialty: 'Global Street & Sandwich',
      totalOrders: 1923,
      products: 8,
      experience: 6,
      tags: ['Street', 'Fusion', 'Quick'],
      bio: 'Creative chef specializing in global street food fusion, bringing flavors from around the world into innovative sandwich creations.',
    },
    {
      id: '4',
      name: 'Jean-Pierre Dubois',
      rating: 4.7,
      specialty: 'Traditional French Patisserie',
      totalOrders: 1654,
      products: 10,
      experience: 12,
      tags: ['French', 'Pastry', 'Classic'],
      bio: 'Classically trained French pastry chef with 12 years of experience in creating exquisite pastries and desserts.',
    },
  ];
}

export async function getChef(id: string): Promise<Chef | null> {
  const chefs = await getTopChefs();
  return chefs.find(chef => chef.id === id) || null;
}

export async function getAllChefIds(): Promise<string[]> {
  const chefs = await getTopChefs();
  return chefs.map(chef => chef.id);
}

export async function getVendors(): Promise<Vendor[]> {
  const baseUrl =
    'http://ec2-13-250-197-155.ap-southeast-1.compute.amazonaws.com';
  const url = `${baseUrl}/vendors`;

  try {
    const res = await fetch(url, { method: 'GET', headers: { Accept: 'application/json' } });
    if (!res.ok) {
      console.warn(`getVendors: ${res.status} ${res.statusText}`);
      return [];
    }

    const json = await res.json();
    if (!Array.isArray(json)) {
      console.warn('getVendors: unexpected response format (not an array)');
      return [];
    }

    // Return the raw vendor objects (typed as Vendor[])
    return json as Vendor[];
  } catch (error) {
    console.error('getVendors error:', error);
    return [];
  }
}

export async function getVendor(id: string | number): Promise<Vendor | null> {
  const vendors = await getVendors();
  const idNum = Number(id);
  if (Number.isNaN(idNum)) return null;
  return vendors.find(v => v.vendorId === idNum) || null;
}

export async function getAllVendorIds(): Promise<string[]> {
  const vendors = await getVendors();
  return vendors
    .map(v => (v.vendorId !== undefined && v.vendorId !== null ? String(v.vendorId) : ''))
    .filter(Boolean);
}

export type Vendor = {
  vendorId: number;
  name: string;
  description?: string | null;
  supportedCategories?: string[];
  schedule?: unknown | null;
  image?: string | null;
  address1?: string | null;
  address2?: string | null;
  state?: string | null;
  landmark?: string | null;
  pincode?: string | null;
  contactName?: string | null;
  phoneNumber?: string | null;
  email?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  createdAt?: string | null;
  scheduleUpdated?: string | null;
};

/**
 * Fetch vendor details from remote service.
 * Returns Vendor object on success or null on failure.
 */
export async function getVendorDetails(vendorId: number): Promise<Vendor | null> {
  const baseUrl =
    'http://ec2-13-250-197-155.ap-southeast-1.compute.amazonaws.com';
  const url = `${baseUrl}/vendors/${encodeURIComponent(String(vendorId))}`;

  try {
    const res = await fetch(url, { method: 'GET', headers: { Accept: 'application/json' } });
    if (!res.ok) {
      // non-2xx
      console.warn(`getVendorDetails: ${res.status} ${res.statusText}`);
      return null;
    }
    const json = await res.json();
    return json as Vendor;
  } catch (error) {
    console.error('getVendorDetails error:', error);
    return null;
  }
}

export type CategoryDetail = {
  Category?: string[];
  Cuisinename?: string;
  SubCategory?: string[];
  regionCategory?: string;
};

export type Media = {
  id: number;
  mediaType: string;
  url: string;
  description?: string | null;
  mimeType?: string | null;
  width?: number | null;
  height?: number | null;
  sizeKb?: number | null;
  durationSeconds?: number | null;
  resolution?: string | null;
  storageProvider?: string | null;
  validationStatus?: string | null;
  validationError?: string | null;
};

export type WeeklySchedule = {
  tz?: string;
  end?: string;
  start?: string;
  stock?: number;
  day_of_week?: string[];
};

export type ProductSchedule = {
  weekly_schedules?: WeeklySchedule[];
};

export type Product = {
  id: number;
  name: string;
  sku?: string | null;
  price?: number | null;
  vendorId?: number | null;
  available?: boolean;
  description?: string | null;
  categoryDetails?: CategoryDetail[];
  schedule?: ProductSchedule | null;
  useVendorSchedule?: boolean;
  media?: Media[];
};

/**
 * Fetch products for a specific vendor.
 * Returns Product[] on success or null on failure.
 */
export async function getVendorProducts(vendorId: number): Promise<Product[] | null> {
  const baseUrl =
    'http://ec2-13-250-197-155.ap-southeast-1.compute.amazonaws.com';
  const url = `${baseUrl}/products/vendor/${encodeURIComponent(String(vendorId))}`;

  try {
    const res = await fetch(url, { method: 'GET', headers: { Accept: 'application/json' } });
    if (!res.ok) {
      console.warn(`getVendorProducts: ${res.status} ${res.statusText}`);
      return null;
    }
    const json = await res.json();
    // basic validation: expecting an array
    if (!Array.isArray(json)) {
      console.warn('getVendorProducts: unexpected response format (not an array)');
      return null;
    }
    return json as Product[];
  } catch (error) {
    console.error('getVendorProducts error:', error);
    return null;
  }
}
