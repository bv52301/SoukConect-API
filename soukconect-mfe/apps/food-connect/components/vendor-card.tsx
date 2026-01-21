import React from 'react';
import Link from 'next/link';
import { Star, Clock, MapPin } from 'lucide-react';

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

export function VendorCard({ vendor }: { vendor: Vendor }) {
  return (
    <Link href={`/chefs/${vendor.vendorId}`}>
      <div className="bg-white rounded-lg overflow-hidden shadow-md hover:shadow-lg transition-shadow cursor-pointer h-full">
        {/* Image Container */}
        <div className="relative aspect-video overflow-hidden bg-gray-200">
          <img
            src={vendor.image}
            alt={vendor.name}
            className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
          />
        </div>

        {/* Content Container */}
        <div className="p-4">
          {/* Title */}
          <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
            {vendor.name}
          </h3>

          {/* Categories */}
          <div className="flex flex-wrap gap-1 mb-3">
            {vendor.supportedCategories.slice(0, 2).map((cat, idx) => (
              <span
                key={idx}
                className="text-xs bg-orange-100 text-orange-700 px-2 py-1 rounded"
              >
                {cat}
              </span>
            ))}
            {vendor.supportedCategories.length > 2 && (
              <span className="text-xs text-gray-500">
                +{vendor.supportedCategories.length - 2}
              </span>
            )}
          </div>

          {/* Rating and Info */}
          <div className="flex items-center gap-4 text-sm text-gray-600 mb-3">
            <div className="flex items-center gap-1">
              <Star className="w-4 h-4 fill-orange-400 text-orange-400" />
              <span>4.5</span>
            </div>
            <div className="flex items-center gap-1">
              <Clock className="w-4 h-4" />
              <span>30 mins</span>
            </div>
            <div className="flex items-center gap-1">
              <MapPin className="w-4 h-4" />
              <span>1.2 km</span>
            </div>
          </div>

          {/* Address */}
          <div className="text-sm text-gray-600 mb-2">
            <p className="font-medium">{vendor.address1}</p>
            <p className="text-xs">{vendor.address2}, {vendor.state}</p>
          </div>

          {/* Description */}
          <p className="text-sm text-gray-500 line-clamp-2 mb-3">
            {vendor.description}
          </p>

          {/* Contact */}
          <div className="text-xs text-gray-500 border-t pt-2 mb-3">
            <p>📞 {vendor.phoneNumber}</p>
          </div>

          {/* CTA Button */}
          <button
            className="w-full bg-gray-900 hover:bg-gray-800 text-white text-center font-semibold py-2.5 rounded-lg transition"
            onClick={(e) => {
              // Link will handle navigation, this just prevents event bubbling
              e.preventDefault();
            }}
          >
            View Menu
          </button>
        </div>
      </div>
    </Link>
  );
}