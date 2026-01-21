'use client';

import { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface Slide {
  id: number;
  badge: string;
  badgeIcon: string;
  title: string;
  subtitle: string;
  description: string;
  primaryButton: string;
  secondaryButton: string;
  bgGradient: string;
  emoji1: string;
  emoji2: string;
  emoji3: string;
}

const slides: Slide[] = [
  {
    id: 1,
    badge: 'SEASONAL SPECIAL',
    badgeIcon: '🎊',
    title: 'Holiday Party',
    subtitle: 'Planning Made Easy',
    description: 'From gourmet catering to event planning, make your celebrations unforgettable with Party Connect',
    primaryButton: 'Plan Your Party',
    secondaryButton: 'Browse Caterers',
    bgGradient: 'from-blue-500 via-purple-500 to-purple-600',
    emoji1: '🎉',
    emoji2: '🍰',
    emoji3: '🎈',
  },
  {
    id: 2,
    badge: 'NEW LAUNCH',
    badgeIcon: '🎊',
    title: 'Introducing',
    subtitle: 'Reno Connect',
    description: 'Your trusted home improvement hub connecting you with skilled craftspeople and renovation experts',
    primaryButton: 'Explore Reno Connect',
    secondaryButton: 'Join as Professional',
    bgGradient: 'from-teal-400 via-cyan-400 to-teal-500',
    emoji1: '🏠',
    emoji2: '🔨',
    emoji3: '🎨',
  },
  {
    id: 3,
    badge: 'Soukconect PROMISE',
    badgeIcon: '✨',
    title: 'More Than a',
    subtitle: 'Marketplace',
    description: 'Your trusted marketplace for every need - connecting passionate creators with discerning consumers across the globe',
    primaryButton: 'Explore All Brands',
    secondaryButton: 'Join Community',
    bgGradient: 'from-blue-400 via-cyan-400 to-blue-500',
    emoji1: '🎯',
    emoji2: '🌍',
    emoji3: '❤️',
  },
  {
    id: 4,
    badge: 'LIMITED TIME OFFER',
    badgeIcon: '🔥',
    title: 'Summer Sale',
    subtitle: 'Up to 40% Off',
    description: 'Discover amazing deals on artisanal foods, fresh ingredients, and gourmet treats from our top-rated makers',
    primaryButton: 'Shop Summer Deals',
    secondaryButton: 'View All Offers',
    bgGradient: 'from-orange-400 via-red-400 to-orange-500',
    emoji1: '🍪',
    emoji2: '🧀',
    emoji3: '🥐',
  },
];

export function HeroCarousel() {
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isAutoPlaying, setIsAutoPlaying] = useState(true);

  // Auto-play carousel
  useEffect(() => {
    if (!isAutoPlaying) return;

    const interval = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % slides.length);
    }, 5000); // Change slide every 5 seconds

    return () => clearInterval(interval);
  }, [isAutoPlaying]);

  const goToSlide = (index: number) => {
    setCurrentSlide(index);
    setIsAutoPlaying(false); // Stop auto-play when user manually changes
  };

  const goToPrevious = () => {
    setCurrentSlide((prev) => (prev - 1 + slides.length) % slides.length);
    setIsAutoPlaying(false);
  };

  const goToNext = () => {
    setCurrentSlide((prev) => (prev + 1) % slides.length);
    setIsAutoPlaying(false);
  };

  const slide = slides[currentSlide];

  return (
    <section className="relative overflow-hidden">
      {/* Carousel Container */}
      <div className={`relative bg-gradient-to-r ${slide.bgGradient} transition-all duration-700 ease-in-out`}>
        {/* Background Decorative Elements */}
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute left-10 top-20 w-32 h-32 bg-white/10 rounded-full blur-3xl animate-pulse" />
          <div className="absolute right-20 bottom-10 w-40 h-40 bg-white/5 rounded-full blur-3xl animate-pulse delay-1000" />
          <div className="absolute left-1/2 top-1/3 w-24 h-24 bg-white/10 rounded-full blur-2xl" />
        </div>

        <div className="relative max-w-7xl mx-auto px-4 py-12 md:py-16 min-h-[500px] flex items-center">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center w-full">
            {/* Left Content */}
            <div className="text-left space-y-4 animate-fadeIn">
              {/* Badge */}
              <div className="inline-flex items-center gap-2 bg-yellow-400 text-gray-900 px-3 py-1.5 rounded-full text-xs font-bold shadow-lg">
                <span>{slide.badgeIcon}</span>
                <span>{slide.badge}</span>
              </div>

              {/* Title */}
              <div className="space-y-">
                <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-white leading-tight">
                  {slide.title}
                </h1>
                <h2 className="text-4xl md:text-5xl lg:text-6xl font-bold text-yellow-300 leading-tight">
                  {slide.subtitle}
                </h2>
              </div>

              {/* Description */}
              <p className="text-white/90 text-lg md:text-lg max-w-xl leading-relaxed">
                {slide.description}
              </p>

              {/* CTA Buttons */}
              <div className="flex flex-col sm:flex-row gap-3 pt-2">
                <button className="bg-yellow-400 hover:bg-yellow-500 text-gray-900 font-bold px-6 py-3 rounded-lg transition-all transform hover:scale-105 shadow-lg text-sm">
                  {slide.primaryButton}
                </button>
                <button className="bg-white/20 hover:bg-white/30 backdrop-blur-sm text-white font-bold px-6 py-3 rounded-lg border-2 border-white/30 transition-all transform hover:scale-105">
                  {slide.secondaryButton}
                </button>
              </div>
            </div>

            {/* Right Decorative Emojis */}
            <div className="hidden lg:flex justify-center items-center relative h-full min-h-[400px]">
              <div className="absolute animate-float">
                <span className="text-7xl drop-shadow-2xl">{slide.emoji1}</span>
              </div>
              <div className="absolute top-16 right-16 animate-float-delayed">
                <span className="text-5xl drop-shadow-2xl">{slide.emoji2}</span>
              </div>
              <div className="absolute bottom-16 right-24 animate-float-slow">
                <span className="text-5xl drop-shadow-2xl">{slide.emoji3}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Navigation Arrows */}
        <button
          onClick={goToPrevious}
          className="absolute left-4 top-1/2 -translate-y-1/2 bg-white/20 hover:bg-white/30 backdrop-blur-sm text-white p-3 rounded-full transition-all transform hover:scale-110"
          aria-label="Previous slide"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>

        <button
          onClick={goToNext}
          className="absolute right-4 top-1/2 -translate-y-1/2 bg-white/20 hover:bg-white/30 backdrop-blur-sm text-white p-3 rounded-full transition-all transform hover:scale-110"
          aria-label="Next slide"
        >
          <ChevronRight className="w-6 h-6" />
        </button>

        {/* Dot Indicators */}
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex gap-3">
          {slides.map((_, index) => (
            <button
              key={index}
              onClick={() => goToSlide(index)}
              className={`h-3 rounded-full transition-all ${
                index === currentSlide
                  ? 'bg-yellow-400 w-8'
                  : 'bg-white/50 hover:bg-white/70 w-3'
              }`}
              aria-label={`Go to slide ${index + 1}`}
            />
          ))}
        </div>
      </div>
    </section>
  );
}
