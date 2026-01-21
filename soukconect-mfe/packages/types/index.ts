// User Types
export interface User {
  id: string;
  email: string;
  name: string;
  avatar?: string;
  role: 'customer' | 'vendor' | 'admin';
  createdAt: Date;
  updatedAt: Date;
}

// Chef Types
export interface Chef {
  id: string;
  name: string;
  rating: number;
  specialty: string;
  totalOrders: number;
  products: number;
  experience: number;
  tags: string[];
  bio: string;
  avatar?: string;
  verified?: boolean;
}

// Product Types
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  rating: number;
  reviews: number;
  chefId: string;
  chefName: string;
  category: string;
  tags: string[];
  inStock: boolean;
  emoji?: string;
  badge?: string;
  images?: string[];
  createdAt: Date;
  updatedAt: Date;
}

// Cart Types
export interface CartItem {
  productId: string;
  product: Product;
  quantity: number;
  price: number;
  subtotal: number;
}

export interface Cart {
  id: string;
  userId: string;
  items: CartItem[];
  subtotal: number;
  tax: number;
  shipping: number;
  total: number;
  updatedAt: Date;
}

// Order Types
export interface Order {
  id: string;
  userId: string;
  items: CartItem[];
  status: OrderStatus;
  subtotal: number;
  tax: number;
  shipping: number;
  total: number;
  createdAt: Date;
  updatedAt: Date;
}

export type OrderStatus = 
  | 'pending'
  | 'confirmed'
  | 'preparing'
  | 'shipped'
  | 'delivered'
  | 'cancelled';