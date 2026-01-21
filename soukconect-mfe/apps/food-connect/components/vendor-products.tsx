'use client';

import React from 'react';
import { Plus } from 'lucide-react';

// Match the actual data structure from your API/library
interface Media {
  id?: number;
  mediaType?: string;
  url?: string;
  description?: string | null;
}

interface CategoryDetail {
  Category?: string[];
  Cuisinename?: string;
  SubCategory?: string[];
  regionCategory?: string;
}

interface Schedule {
  weekly_schedules?: Array<{
    tz?: string;
    end?: string;
    start?: string;
    stock?: number;
    day_of_week?: string[];
  }>;
}

// Make ALL properties optional to match the actual data from your library
interface Product {
  id?: number;
  name?: string;
  sku?: string | null;
  price?: number | null;
  vendorId?: number | null; // Make optional
  available?: boolean | null;
  description?: string | null;
  categoryDetails?: CategoryDetail[] | null;
  schedule?: Schedule | null;
  useVendorSchedule?: boolean;
  media?: Media[] | null;
}

interface VendorProductsProps {
  products?: Product[] | null;
  onAddToCart?: (product: Product) => void;
}

export function VendorProducts({ products = [], onAddToCart }: VendorProductsProps) {
  const handleAddToCart = (product: Product) => {
    if (onAddToCart) {
      onAddToCart(product);
    } else {
      console.log('Added to cart:', product.name);
    }
  };

  // Safety check and filter valid products
  const validProducts = Array.isArray(products)
    ? products.filter(p => p && p.id && p.name && (p.price ?? 0) > 0)
    : [];

  if (validProducts.length === 0) {
    return (
      <div className="text-center py-16">
        <p className="text-gray-500 text-lg">No products available</p>
      </div>
    );
  }

  return (
    <div className="py-8 px-4 max-w-7xl mx-auto">
      {/* Products Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {validProducts.map((product) => (
          <div
            key={product.id}
            className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow duration-300 overflow-hidden"
          >
            <div className="p-6">
              {/* Product Header with Image and Title */}
              <div className="flex gap-4 mb-4">
                {/* Product Image */}
                <div className="flex-shrink-0">
                  <div className="w-32 h-32 rounded-lg overflow-hidden bg-gray-100">
                    {product.media && Array.isArray(product.media) && product.media.length > 0 && product.media[0]?.url ? (
                      <img
                        src={product.media[0].url}
                        alt={product.name || 'Product'}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="128" height="128"%3E%3Crect fill="%23f3f4f6" width="128" height="128"/%3E%3Ctext x="50%25" y="50%25" fill="%239ca3af" text-anchor="middle" dy=".3em" font-size="14"%3ENo Image%3C/text%3E%3C/svg%3E';
                        }}
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-gray-400 text-sm">
                        No Image
                      </div>
                    )}
                  </div>
                </div>

                {/* Product Info */}
                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
                    {product.name || 'Unnamed Product'}
                  </h3>
                  
                  {/* Description */}
                  {product.description && (
                    <p className="text-sm text-gray-600 line-clamp-3 mb-3">
                      {product.description}
                    </p>
                  )}
                </div>
              </div>

              {/* Price and Add Button */}
              <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                <div>
                  <span className="text-2xl font-bold text-gray-900">
                    ${((product.price as number) || 0).toFixed(2)}
                  </span>
                </div>

                <button
                  onClick={() => handleAddToCart(product)}
                  disabled={product.available === false}
                  className={`
                    w-10 h-10 rounded-full flex items-center justify-center
                    transition-all duration-200
                    ${product.available !== false 
                      ? 'bg-green-500 hover:bg-green-600 text-white shadow-md hover:shadow-lg' 
                      : 'bg-gray-300 text-gray-500 cursor-not-allowed'
                    }
                  `}
                  aria-label={`Add ${product.name} to cart`}
                >
                  <Plus size={20} />
                </button>
              </div>

              {/* Availability Status */}
              {product.available === false && (
                <div className="mt-3">
                  <span className="text-xs text-red-600 font-medium">
                    Currently Unavailable
                  </span>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// Example usage with sample data
const VendorProductsExample = () => {
  const sampleProducts: Product[] = [
    {
      id: 5,
      name: "Chettinad Chicken Masala",
      sku: "AC-001",
      price: 16,
      vendorId: 4,
      available: true,
      description: "A fiery, aromatic curry made with fresh chicken and a complex blend of roasted Chettinad spices.",
      categoryDetails: [
        {
          Category: ["Indian"],
          Cuisinename: "Asian",
          SubCategory: ["South Indian"],
          regionCategory: "Chettinad"
        }
      ],
      schedule: {
        weekly_schedules: [
          {
            tz: "Asia/Singapore",
            end: "17:00",
            start: "08:00",
            stock: 20,
            day_of_week: ["Sun"]
          }
        ]
      },
      useVendorSchedule: false,
      media: [
        {
          id: 4,
          mediaType: "IMAGE",
          url: "https://bing.com/th?id=OSK.b2ab0b7ccd417709f2a8361477e5d4fd",
          description: null
        }
      ]
    },
    {
      id: 6,
      name: "KVR Uppuma",
      sku: "AC-002",
      price: 7.00,
      vendorId: 4,
      available: true,
      description: "Savory South Indian delight with sambar and chutneys.",
      categoryDetails: [
        {
          Category: ["Indian"],
          Cuisinename: "Asian",
          SubCategory: ["South Indian"]
        }
      ],
      schedule: {
        weekly_schedules: []
      },
      useVendorSchedule: false,
      media: []
    },
    {
      id: 7,
      name: "KVR Pongal",
      sku: "AC-003",
      price: 7.00,
      vendorId: 4,
      available: true,
      description: "Soft Pongal served with spicy sambar and chutneys.",
      categoryDetails: [
        {
          Category: ["Indian"],
          Cuisinename: "Asian",
          SubCategory: ["South Indian"]
        }
      ],
      schedule: {
        weekly_schedules: []
      },
      useVendorSchedule: true,
      media: []
    }
  ];

  const handleAddToCart = (product: Product) => {
    alert(`Added ${product.name} to cart!`);
  };

  return <VendorProducts products={sampleProducts} onAddToCart={handleAddToCart} />;
};

export default VendorProductsExample;