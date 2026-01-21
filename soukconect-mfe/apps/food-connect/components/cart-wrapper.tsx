'use client';

import { useState } from 'react';
import Link from 'next/link';
import { VendorProducts } from '@/components/vendor-products';

interface CartItem {
  productId: number;
  name: string;
  price: number;
  quantity: number;
  vendorId: number;
}

interface CartWrapperProps {
  chef: any;
  sampleProducts: any[] | null;
  randomBg: string;
}

export default function CartWrapper({ chef, sampleProducts, randomBg }: CartWrapperProps) {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [showCart, setShowCart] = useState(false);

  const handleAddToCart = (product: any) => {
    setCart((prevCart) => {
      // Check if product already exists in cart
      const existingItem = prevCart.find((item) => item.productId === product.id);

      if (existingItem) {
        // Increase quantity
        return prevCart.map((item) =>
          item.productId === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        );
      } else {
        // Add new item
        return [
          ...prevCart,
          {
            productId: product.id,
            name: product.name,
            price: product.price,
            quantity: 1,
            vendorId: product.vendorId,
          },
        ];
      }
    });

    // Show success feedback
    console.log(`Added ${product.name} to cart`);
  };

  const handleRemoveFromCart = (productId: number) => {
    setCart((prevCart) =>
      prevCart.filter((item) => item.productId !== productId)
    );
  };

  const handleUpdateQuantity = (productId: number, quantity: number) => {
    if (quantity <= 0) {
      handleRemoveFromCart(productId);
    } else {
      setCart((prevCart) =>
        prevCart.map((item) =>
          item.productId === productId ? { ...item, quantity } : item
        )
      );
    }
  };

  const cartTotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const cartItemCount = cart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <>
      {/* Cart Summary Button */}
      {cartItemCount > 0 && (
        <div className="fixed bottom-6 right-6 z-40">
          <button
            onClick={() => setShowCart(!showCart)}
            className="bg-green-500 hover:bg-green-600 text-white rounded-full p-4 shadow-lg flex items-center gap-2 font-semibold"
          >
            🛒 {cartItemCount} item{cartItemCount !== 1 ? 's' : ''}
          </button>
        </div>
      )}

      {/* Cart Drawer */}
      {showCart && cartItemCount > 0 && (
        <div className="fixed right-0 top-0 h-full w-96 bg-white shadow-2xl z-50 overflow-y-auto">
          <div className="p-6">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold">Your Cart</h2>
              <button
                onClick={() => setShowCart(false)}
                className="text-gray-500 hover:text-gray-700 text-2xl"
              >
                ✕
              </button>
            </div>

            {/* Cart Items */}
            <div className="space-y-4 mb-6">
              {cart.map((item) => (
                <div key={item.productId} className="border-b pb-4">
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-semibold">{item.name}</h3>
                    <button
                      onClick={() => handleRemoveFromCart(item.productId)}
                      className="text-red-500 hover:text-red-700 text-sm"
                    >
                      Remove
                    </button>
                  </div>
                  <div className="flex justify-between items-center">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleUpdateQuantity(item.productId, item.quantity - 1)}
                        className="bg-gray-200 hover:bg-gray-300 w-8 h-8 rounded"
                      >
                        −
                      </button>
                      <span className="w-8 text-center">{item.quantity}</span>
                      <button
                        onClick={() => handleUpdateQuantity(item.productId, item.quantity + 1)}
                        className="bg-gray-200 hover:bg-gray-300 w-8 h-8 rounded"
                      >
                        +
                      </button>
                    </div>
                    <span className="font-semibold">
                      ${(item.price * item.quantity).toFixed(2)}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* Cart Total */}
            <div className="border-t pt-4 mb-6">
              <div className="flex justify-between text-lg font-bold mb-4">
                <span>Total:</span>
                <span>${cartTotal.toFixed(2)}</span>
              </div>
              <button className="w-full bg-green-500 hover:bg-green-600 text-white font-bold py-3 rounded-lg transition">
                Proceed to Checkout
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Main Content */}
      <main className="min-h-screen bg-gray-50">
        {/* Chef Header */}
        <section className="bg-white border-b">
          <div className="max-w-6xl mx-auto px-4 py-12">
            <div className="flex flex-col md:flex-row gap-8 items-start">
              {/* Avatar */}
              <div className={`${randomBg} w-32 h-32 rounded-full flex items-center justify-center shadow-xl`}>
                <span className="text-6xl">👨‍🍳</span>
              </div>

              {/* Info */}
              <div className="flex-1">
                <div className="mb-4">
                  {/* Back to Category Vendors - links to home with category query param if available */}
                  {chef?.supportedCategories && chef.supportedCategories.length > 0 ? (
                    <Link
                      href={`/?category=${encodeURIComponent(chef.supportedCategories[0])}`}
                      className="inline-block bg-gray-200 hover:bg-gray-300 text-gray-800 px-3 py-1 rounded-lg mb-2"
                    >
                      ← Back to category vendors
                    </Link>
                  ) : (
                    <button
                      onClick={() => window.history.back()}
                      className="inline-block bg-gray-200 hover:bg-gray-300 text-gray-800 px-3 py-1 rounded-lg mb-2"
                    >
                      ← Back
                    </button>
                  )}
                </div>
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h1 className="text-4xl font-bold mb-2">{chef.name}</h1>
                    <p className="text-gray-600 text-lg">{chef.address1}</p>
                  </div>
                  <div className="bg-yellow-400 text-gray-900 font-bold px-4 py-2 rounded-full">
                    ⭐ {chef.address2}
                  </div>
                </div>

                <p className="text-gray-700 mb-6">{chef.description}</p>

                {/* Tags */}
                <div className="flex flex-wrap gap-2">
                  {chef?.supportedCategories?.map((tag: any) => (
                    <span
                      key={tag}
                      className="bg-gray-100 text-gray-700 px-4 py-2 rounded-full text-sm"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Products Section */}
        <section className="py-12 px-4">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold mb-8">Products by {chef.name}</h2>
            <VendorProducts products={sampleProducts} onAddToCart={handleAddToCart} />
          </div>
        </section>
      </main>
    </>
  );
}