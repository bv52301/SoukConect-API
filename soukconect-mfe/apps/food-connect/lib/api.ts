const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000/api';

console.log('DEBUG - API_BASE_URL:', API_BASE_URL); // Debug log

export const categoryApi = {
  async getCategories() {
    const url = `${API_BASE_URL}/cuisines/categories`;
    console.log('DEBUG - Fetching from:', url); // Debug log
    
    const res = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      
      // Remove cache option - it's for server-side only
    });
    
    if (!res.ok) throw new Error(`Failed to fetch categories: ${res.status}`);
    return res.json();
  },

  async getCategoryById(id: string) {
    const url = `${API_BASE_URL}/cuisines/categories/${id}`;
    const res = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    if (!res.ok) throw new Error(`Category ${id} not found`);
    return res.json();
  }
};

export const vendorApi = {
  async getVendorsByCategory(category: string) {
    const url = `${API_BASE_URL}/vendors/by-category?category=${category}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to fetch vendors');
    return res.json();
  }
};