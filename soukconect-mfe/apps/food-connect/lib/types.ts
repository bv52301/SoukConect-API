/* export interface Chef {
  id: string;
  name: string;
  rating: number;
  specialty: string;
  totalOrders: number;
  products: number;
  experience: number;
  tags: string[];
  bio: string;
} */

  // Re-export from shared types package
export type { Chef, Product, Cart, CartItem } from '@soukconect/types';