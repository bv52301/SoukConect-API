"use client";
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import React, { useState } from 'react';
import SignInModal from './components/login/signin';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

const metadata: Metadata = {
  title: 'Soukconect - More Than a Marketplace',
  description: 'Your trusted marketplace for every need',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [isSignInOpen, setIsSignInOpen] = useState(false);
  return (
    <html lang="en">
      <body className={inter.className}>
        {/* Navbar */}
        <nav className="bg-blue-600 text-white shadow-lg">
          <div className="max-w-7xl mx-auto px-4">
            <div className="flex justify-between items-center h-16">
              <a href="/" className="flex items-center gap-2 font-bold text-xl">
                <span className="text-2xl">✨</span>
                <span>Soukconect</span>
              </a>
              
              <div className="hidden md:flex items-center gap-6">
                <a href="/" className="hover:text-yellow-300 transition">Home</a>
                <a href="/about" className="hover:text-yellow-300 transition">About</a>
                <a href="/contact" className="hover:text-yellow-300 transition">Contact</a>
              </div>

              <button onClick={() => setIsSignInOpen(true)} className="bg-yellow-400 text-gray-900 px-4 py-2 rounded-lg font-semibold hover:bg-yellow-500 transition">
                Join Now
              </button>
            </div>
          </div>
        </nav>

        {children}

        {/* Footer */}
        <footer className="bg-gray-900 text-white py-12 px-4 mt-20">
          <div className="max-w-6xl mx-auto">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
              <div>
                <h3 className="font-bold text-lg mb-4">Soukconect</h3>
                <p className="text-gray-400 text-sm">
                  Connecting passionate creators with discerning consumers
                </p>
              </div>
              
              <div>
                <h4 className="font-semibold mb-4">Our Brands</h4>
                <ul className="space-y-2 text-gray-400 text-sm">
                  <li>Food Connect</li>
                  <li>Reno Connect</li>
                  <li>Party Connect</li>
                </ul>
              </div>

              <div>
                <h4 className="font-semibold mb-4">Company</h4>
                <ul className="space-y-2 text-gray-400 text-sm">
                  <li><a href="/about">About</a></li>
                  <li><a href="/careers">Careers</a></li>
                  <li><a href="/contact">Contact</a></li>
                </ul>
              </div>

              <div>
                <h4 className="font-semibold mb-4">Legal</h4>
                <ul className="space-y-2 text-gray-400 text-sm">
                  <li><a href="/terms">Terms</a></li>
                  <li><a href="/privacy">Privacy</a></li>
                </ul>
              </div>
            </div>

            <div className="border-t border-gray-800 pt-8 text-center text-gray-400 text-sm">
              <p>© 2025 Soukconect. All rights reserved.</p>
            </div>
          </div>
        </footer>

        <SignInModal
          isOpen={isSignInOpen}
          onClose={() => setIsSignInOpen(false)}
        />
      </body>
    </html>
  );
}