import Link from 'next/link';
import type { Chef } from '../lib/types';

interface ChefCardProps {
  chef: Chef;
}

export function ChefCard({ chef }: ChefCardProps) {
  const bgColors = [
    'bg-gradient-to-br from-orange-400 to-orange-500',
    'bg-gradient-to-br from-teal-400 to-teal-500',
    'bg-gradient-to-br from-green-400 to-green-500',
    'bg-gradient-to-br from-purple-400 to-purple-500',
  ];

  const randomBg = bgColors[parseInt(chef.id) % bgColors.length];

  return (
    <div className="bg-white rounded-2xl shadow-lg overflow-hidden hover:shadow-xl transition-shadow">
      {/* Chef Avatar */}
      <div className="flex justify-center pt-8 mb-4">
        <div className={`${randomBg} w-24 h-24 rounded-full flex items-center justify-center border-4 border-white shadow-lg`}>
          <span className="text-4xl">👨‍🍳</span>
        </div>
      </div>

      {/* Chef Info */}
      <div className="px-6 pb-6">
        <div className="text-center mb-3">
          <h3 className="font-bold text-lg mb-1">{chef.name}</h3>
          <div className="flex items-center justify-center gap-1 text-yellow-500 mb-1">
            {'⭐'.repeat(Math.floor(chef.rating))}
            <span className="text-gray-600 text-sm ml-1">{chef.rating}</span>
          </div>
          <p className="text-gray-500 text-sm">{chef.specialty}</p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-2 mb-4 text-center">
          <div className="bg-gray-50 rounded-lg py-2">
            <div className="font-bold text-lg">{chef.totalOrders.toLocaleString()}</div>
            <div className="text-xs text-gray-500">Orders</div>
          </div>
          <div className="bg-gray-50 rounded-lg py-2">
            <div className="font-bold text-lg">{chef.products}</div>
            <div className="text-xs text-gray-500">Products</div>
          </div>
          <div className="bg-gray-50 rounded-lg py-2">
            <div className="font-bold text-lg">{chef.experience}y</div>
            <div className="text-xs text-gray-500">Exp</div>
          </div>
        </div>

        {/* Tags */}
        <div className="flex flex-wrap gap-2 mb-4">
          {chef.tags.slice(0, 3).map((tag:any) => (
            <span
              key={tag}
              className="bg-blue-100 text-blue-700 text-xs px-3 py-1 rounded-full"
            >
              {tag}
            </span>
          ))}
        </div>

        {/* CTA Button */}
        <Link
          href={`/chefs/${chef.id}`}
          className="block bg-gray-900 hover:bg-gray-800 text-white text-center font-semibold py-2.5 rounded-lg transition"
        >
          View Products
        </Link>
      </div>
    </div>
  );
}