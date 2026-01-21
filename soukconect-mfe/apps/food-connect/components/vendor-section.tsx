'use client';
import React, { useEffect, useRef, useState } from 'react';
import { vendorApi } from '@/lib/api';
import { VendorCard } from './vendor-card';

interface Vendor {
  vendorId: number;
  name: string;
  description: string;
  image: string;
  address1: string;
  address2: string;
  state: string;
  supportedCategories: string[];
  phoneNumber: string;
  email: string;
}

interface VendorsSectionProps {
  category: string;
  onClose?: () => void;
}

export function VendorsSection({ category, onClose }: VendorsSectionProps) {
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

   useEffect(() => {
    // Cancel previous request if it exists
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    const fetchVendors = async () => {
      try {
        setLoading(true);
        setError(null);
        
        abortControllerRef.current = new AbortController();
        console.log('Fetching vendors for:', category);
        
        const data = await vendorApi.getVendorsByCategory(category);
        console.log('Vendors fetched:', data.length);
        setVendors(Array.isArray(data) ? data : []);
      } catch (err) {
        if (err instanceof Error && err.name === 'AbortError') {
          console.log('Request cancelled');
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load vendors');
        setVendors([]);
      } finally {
        setLoading(false);
      }
    };

    fetchVendors();

    // Cleanup function
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [category]);

  if (loading) {
    return (
      <div className="py-12 text-center">
        <p className="text-gray-500">Loading vendors...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-12 text-center">
        <p className="text-red-500">{error}</p>
      </div>
    );
  }

  if (vendors.length === 0) {
    return (
      <div className="py-12 text-center">
        <p className="text-gray-500">No vendors found for {category}</p>
      </div>
    );
  }

  return (
    <section className="py-12 px-4 max-w-7xl mx-auto">
      <div className="mb-8 flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-bold text-gray-900 mb-2">
            🏪 {category} Vendors
          </h2>
          <p className="text-gray-600">
            Showing {vendors.length} vendors in {category}
          </p>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            className="bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded-lg"
          >
            ← Back to Categories
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {vendors.map((vendor) => (
          <VendorCard key={vendor.vendorId} vendor={vendor} />
        ))}
      </div>
    </section>
  );
}