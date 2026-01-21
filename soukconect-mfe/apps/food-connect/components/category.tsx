'use client';
import React, { useEffect, useState } from 'react';
import { categoryApi } from '@/lib/api';

interface Category {
  category: string;
  imageUrl: string;
}

const getColorForCategory = (categoryName: string): [string, string] => {
  const colorMap: Record<string, [string, string]> = {
    'noodles': ['#FF6B6B', '#FF8E8E'],
    'western': ['#4ECDC4', '#6FE0D6'],
    'burgers': ['#FFD93D', '#FFE66D'],
    'dessert': ['#FF6BA8', '#FF8FC7'],
    'asian': ['#95E1D3', '#B5F1E3'],
    'seafood': ['#3DC2EC', '#6DD5F5'],
    'chinese': ['#FA5252', '#FC7474'],
    'fried-chicken': ['#FFAA00', '#FFC133'],
    'chicken': ['#FF8787', '#FFA3A3'],
    'beverages': ['#51CF66', '#74E089'],
    'fast-food': ['#FF922B', '#FFB05F'],
    'snack': ['#FAB005', '#FCC419'],
    'halal': ['#37B24D', '#51CF66'],
    'singaporean': ['#F783AC', '#FAA2C1'],
    'indian': ['#F06595', '#F783AC'],
    'local': ['#FF6B6B', '#FF8E8E'],
    'japanese': ['#20C997', '#38D9A9'],
    'pasta': ['#FFA94D', '#FFC078'],
    'american': ['#FF6B6B', '#FF8E8E'],
    'argentinian': ['#4ECDC4', '#6FE0D6'],
    'british': ['#FFD93D', '#FFE66D'],
    'canadian': ['#FF6BA8', '#FF8FC7'],
  };
  
  const key = categoryName.toLowerCase();
  return colorMap[key] || ['#868E96', '#ADB5BD'];
};

export default function FoodCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await categoryApi.getCategories();
        setCategories(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load categories');
      } finally {
        setLoading(false);
      }
    };

    fetchCategories();
  }, []);

  if (loading) return <div className="py-12 px-4 text-center">Loading categories...</div>;
  if (error) return <div className="py-12 px-4 text-center text-red-600">{error}</div>;

  return (
    <section className="py-12 px-4 max-w-7xl mx-auto">
      {(() => {
        function SearchWidget() {
          const [query, setQuery] = useState('');
          const [results, setResults] = useState<Category[] | null>(null);

          const handleSearch = () => {
        const q = query.trim().toLowerCase();
        if (!q) {
          setResults(null);
          return;
        }
        // simple lookup: treat matching categories as "vendors" for now
        const matched = categories.filter(c => c.category.toLowerCase().includes(q));
        setResults(matched);
          };

          return (
        <>
          <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div className="flex-1">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900">
            There's something for everyone!
          </h2>
            </div>

            <div className="w-full md:w-1/2 flex items-center gap-2">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleSearch(); }}
            placeholder="Search for category, vendor and products"
            className="flex-1 px-4 py-2 border rounded-lg focus:outline-none"
            aria-label="Search categories vendors products"
          />
          <button
            type="button"
            onClick={handleSearch}
            className="px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700"
          >
            Search
          </button>
            </div>
          </div>

          {results !== null && (
            <div className="mb-6">
          <h4 className="text-lg font-medium mb-3">Vendors matching "{query}"</h4>

          {results.length === 0 ? (
            <div className="text-sm text-gray-500">No vendors found.</div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4 md:gap-6">
              {results.map((v, i) => {
            const hasImage = v.imageUrl && v.imageUrl !== 'None';
            const [c1, c2] = getColorForCategory(v.category);
            return (
              <div key={`${v.category}-${i}`} className="group cursor-pointer">
                <div className="relative aspect-square rounded-2xl overflow-hidden mb-3 shadow-md hover:shadow-xl transition-shadow duration-300">
              {hasImage ? (
                <img src={v.imageUrl} alt={v.category} className="w-full h-full object-cover" />
              ) : (
                <div
                  className="absolute inset-0 flex items-center justify-center text-white font-bold text-lg"
                  style={{ background: `linear-gradient(135deg, ${c1} 0%, ${c2} 100%)` }}
                >
                  <span className="drop-shadow-md">{v.category}</span>
                </div>
              )}

              <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors duration-300" />
                </div>

                <h3 className="text-base md:text-lg font-semibold text-gray-900 group-hover:text-orange-600 transition-colors">
              {v.category}
                </h3>
              </div>
            );
              })}
            </div>
          )}
            </div>
          )}
        </>
          );
        }

        return <SearchWidget />;
      })()}

      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4 md:gap-6">
        {categories.map((category, index) => {
          const hasImage = category.imageUrl && category.imageUrl !== 'None';
          const [color1, color2] = getColorForCategory(category.category);

          return (
            <div
              key={`${category.category}-${index}`}
              data-category={category.category}
              className="group cursor-pointer"
            >
              <div className="relative aspect-square rounded-2xl overflow-hidden mb-3 shadow-md hover:shadow-xl transition-shadow duration-300">
                {hasImage ? (
                  <img
                    src={category.imageUrl}
                    alt={category.category}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div 
                    className="absolute inset-0 flex items-center justify-center text-white font-bold text-lg"
                    style={{
                      background: `linear-gradient(135deg, ${color1} 0%, ${color2} 100%)`
                    }}
                  >
                    <span className="drop-shadow-md">{category.category}</span>
                  </div>
                )}
                
                <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors duration-300" />
              </div>

              <h3 className="text-base md:text-lg font-semibold text-gray-900 group-hover:text-orange-600 transition-colors">
                {category.category}
              </h3>
            </div>
          );
        })}
      </div>
    </section>
  );
}