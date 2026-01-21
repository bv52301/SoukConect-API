"use client";
import { useState } from 'react';
import { Eye, EyeOff, X } from 'lucide-react';

export default function SignInModal({ isOpen = true, onClose = () => {} }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = () => {
    if (!email || !password) return;
    
    setIsLoading(true);
    // Simulate API call
    setTimeout(() => {
      setIsLoading(false);
      console.log('Login attempt:', { email, password });
      // Add your login logic here
    }, 1000);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="relative w-full max-w-md mx-4 bg-white rounded-lg shadow-2xl p-8">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors"
        >
          <X size={24} />
        </button>

        {/* Header */}
        <h1 className="text-3xl font-bold text-gray-900 mb-3">Sign in</h1>
        <p className="text-gray-600 text-sm mb-6">
          Sign Up Now! Discounts and early access to sales for exclusive member only. It pays to belong!
        </p>

        {/* Email Input */}
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Email
          </label>
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
          />
        </div>

        {/* Password Input */}
        <div className="mb-2">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Password
          </label>
          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all pr-10"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
        </div>

        {/* Forgot Password Link */}
        <div className="text-right mb-6">
          <button className="text-sm text-gray-600 hover:text-gray-900 transition-colors">
            Forgot Password?
          </button>
        </div>

        {/* Login Button */}
        <button
          onClick={handleLogin}
          disabled={isLoading || !email || !password}
          className="w-full bg-blue-900 hover:bg-blue-800 disabled:bg-blue-900 disabled:opacity-70 text-white font-bold py-3 rounded-lg transition-colors"
        >
          {isLoading ? 'LOGGING IN...' : 'LOGIN'}
        </button>

        {/* Divider */}
        <div className="flex items-center my-6">
          <div className="flex-1 border-t border-gray-300"></div>
          <span className="px-3 text-gray-500 text-sm">OR</span>
          <div className="flex-1 border-t border-gray-300"></div>
        </div>

        {/* Register Link */}
        <p className="text-center text-gray-600 text-sm mb-4">
          Don't have an account?{' '}
          <button className="font-bold text-gray-900 hover:text-blue-900 transition-colors">
            Register Here
          </button>
        </p>

        {/* Mobile Login Button */}
        <button className="w-full border-2 border-gray-900 text-gray-900 font-bold py-3 rounded-lg hover:bg-gray-50 transition-colors flex items-center justify-center gap-2">
          <span>📱</span>
          Login With Mobile Number
        </button>
      </div>
    </div>
  );
}