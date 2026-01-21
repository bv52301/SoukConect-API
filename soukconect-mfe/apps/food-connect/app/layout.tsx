import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'Food Connect - Artisanal Food Marketplace',
  description: 'Discover artisanal food producers and culinary masters',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <nav className="bg-green-600 text-white shadow-lg">
          <div className="max-w-7xl mx-auto px-4">
            <div className="flex justify-between items-center h-16">
              <a href="/" className="flex items-center gap-2 font-bold text-xl">
                <span className="text-2xl">🍕</span>
                <span>Food Connect</span>
              </a>
              
              <div className="hidden md:flex items-center gap-6">
                <a href="/" className="hover:text-yellow-300 transition">Browse</a>
                <a href="/chefs" className="hover:text-yellow-300 transition">Chefs</a>
                <a href="/cart" className="hover:text-yellow-300 transition">Cart</a>
              </div>

              <div className="flex items-center gap-4">
                <button className="relative">
                  <span className="text-2xl">🛒</span>
                  <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs w-5 h-5 rounded-full flex items-center justify-center font-bold">
                    0
                  </span>
                </button>
                <button className="bg-yellow-400 text-gray-900 px-4 py-2 rounded-lg font-semibold hover:bg-yellow-500 transition">
                  Sign In
                </button>
              </div>
            </div>
          </div>
        </nav>
        
        {children}

        <footer className="bg-gray-900 text-white py-12 px-4 mt-20">
          <div className="max-w-6xl mx-auto text-center">
            <p className="text-gray-400">Part of the Soukconect family</p>
            <p className="mt-2">© 2024 Food Connect. All rights reserved.</p>
          </div>
        </footer>
      </body>
    </html>
  );
}