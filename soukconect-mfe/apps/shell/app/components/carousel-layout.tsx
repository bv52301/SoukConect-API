import React, { useState } from 'react';
import { FullCarousel} from './full-carousel-layout'
import { DualSidebarCarousel} from './dual-sidebar-layout'
import { SingleCardLayout} from './single-card-layout'
import { CardStackLayout} from './card-stack-layout'

const bannerData = {
  banners: [
    {
      id: 'full-carousel',
      type: 'carousel',
      layout: 'full-carousel',
      carouselConfig: { fixedHeight: 500 },
      items: [
        {
          id: 1,
          badge: { text: 'SEASONAL SPECIAL', icon: '🎊', position: 'top-left', display: true },
          title: { text: 'Holiday Party', position: 'left-center', display: true, fontSize: 'text-6xl', alignment: 'text-left' },
          subtitle: { text: 'Planning Made Easy', position: 'left-center', display: true, fontSize: 'text-2xl', alignment: 'text-left' },
          description: { text: 'From gourmet catering to event planning, make your celebrations unforgettable', position: 'left-center', display: true, fontSize: 'text-lg', alignment: 'text-left' },
          primaryButton: { label: 'Plan Your Party', url: '/plan-party', position: 'left-center', display: true, style: 'bg-white text-gray-900' },
          secondaryButton: { label: 'Browse Caterers', url: '/caterers', position: 'left-center', display: true, style: 'border-2 border-white text-white' },
          emoji: { emoji1: '🎉', emoji2: '🍰', emoji3: '🎈', position: 'right-center', display: true, layout: 'vertical' },
          bgGradient: 'linear-gradient(135deg, #3b82f6 0%, #a855f7 50%, #9333ea 100%)'
        },
        {
          id: 2,
          badge: { text: 'LIMITED TIME', icon: '⏰', position: 'top-left', display: true },
          title: { text: 'Summer Adventure', position: 'left-center', display: true, fontSize: 'text-6xl', alignment: 'text-left' },
          subtitle: { text: 'Explore the World', position: 'left-center', display: true, fontSize: 'text-2xl', alignment: 'text-left' },
          description: { text: 'Discover amazing travel packages and exclusive deals for your vacation', position: 'left-center', display: true, fontSize: 'text-lg', alignment: 'text-left' },
          primaryButton: { label: 'Book Now', url: '/book-travel', position: 'left-center', display: true, style: 'bg-white text-gray-900' },
          secondaryButton: { label: 'View Packages', url: '/travel-packages', position: 'left-center', display: true, style: 'border-2 border-white text-white' },
          emoji: { emoji1: '✈️', emoji2: '🏖️', emoji3: '🗺️', position: 'right-center', display: true, layout: 'vertical' },
          bgGradient: 'linear-gradient(135deg, #f59e0b 0%, #ef4444 50%, #ec4899 100%)'
        }
      ]
    },
    {
      id: 'carousel-center-dual-sidebar',
      type: 'split-layout',
      layout: 'carousel-center-dual-sidebar',
      tripleConfig: { fixedHeight: 450, leftWidth: 25, carouselWidth: 50, rightWidth: 25 },
      carousel: {
        items: [
          {
            id: 1,
            badge: { text: 'HOT DEAL', icon: '🔥', position: 'top-center', display: true },
            title: { text: 'MEGA DEAL', position: 'center', display: true, fontSize: 'text-4xl', alignment: 'text-center' },
            subtitle: { text: 'Don\'t miss out', position: 'center', display: true, fontSize: 'text-lg', alignment: 'text-center' },
            description: { text: 'Exclusive offers just for you', position: 'center', display: true, fontSize: 'text-sm', alignment: 'text-center' },
            primaryButton: { label: 'SHOP', url: '/shop-mega', position: 'bottom-center', display: true, style: 'bg-white text-blue-600' },
            secondaryButton: { label: 'Learn More', url: '/mega-info', position: 'bottom-center', display: false, style: 'border-white text-white' },
            emoji: { emoji1: '🎯', emoji2: '💎', emoji3: '⭐', position: 'right-center', display: true, layout: 'vertical' },
            bgGradient: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)'
          }
        ]
      },
      leftSidebar: { title: 'HOT DEALS', bgColor: '#ff6b35', icon: '🔥', items: [{ label: 'Up to 50% OFF', icon: '💯', url: '/deal' }, { label: 'Free Shipping', icon: '🚚', url: '/ship' }, { label: 'Extra 20% OFF', icon: '🎁', url: '/extra' }] },
      rightSidebar: { title: 'TRENDING', bgColor: '#004e89', icon: '⭐', items: [{ label: 'Best Sellers', icon: '🏆', url: '/best' }, { label: 'New Arrivals', icon: '✨', url: '/new' }, { label: 'Top Rated', icon: '💎', url: '/top' }] }
    },
    {
      id: 'carousel-with-single-card',
      type: 'split-layout',
      layout: 'carousel-with-single-card',
      splitConfig: { fixedHeight: 450, carouselWidth: 75, cardStackWidth: 25 },
      carousel: {
        items: [
          {
            id: 1,
            badge: { text: 'VIP EXCLUSIVE', icon: '👑', position: 'top-left', display: true },
            title: { text: 'PREMIUM COLLECTION', position: 'left-center', display: true, fontSize: 'text-5xl', alignment: 'text-left' },
            subtitle: { text: 'Exclusive deals for VIP members', position: 'left-center', display: true, fontSize: 'text-lg', alignment: 'text-left' },
            description: { text: 'Premium products curated just for you', position: 'left-center', display: true, fontSize: 'text-sm', alignment: 'text-left' },
            primaryButton: { label: 'SHOP NOW', url: '/shop-premium', position: 'left-bottom', display: true, style: 'bg-white text-gray-900' },
            secondaryButton: { label: 'View Collection', url: '/premium-collection', position: 'left-bottom', display: true, style: 'border-2 border-white text-white' },
            emoji: { emoji1: '👑', emoji2: '💎', emoji3: '✨', position: 'right-center', display: true, layout: 'vertical' },
            bgGradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
          }
        ]
      },
      singleCard: {
        badge: { text: 'SPECIAL OFFER', icon: '💰', position: 'top-center', display: true },
        title: { text: 'SPECIAL OFFER', position: 'center', display: true, fontSize: 'text-3xl', alignment: 'text-center' },
        subtitle: { text: 'Limited Time Only', position: 'center', display: true, fontSize: 'text-sm', alignment: 'text-center' },
        description: { text: 'Get 30% extra cashback on your first order', position: 'center', display: true, fontSize: 'text-xs', alignment: 'text-center' },
        primaryButton: { label: 'Claim Now', url: '/claim-offer', position: 'bottom-center', display: true, style: 'bg-white text-gray-900' },
        secondaryButton: { label: 'Learn More', url: '/offer-details', position: 'bottom-center', display: true, style: 'border-2 border-white text-white' },
        emoji: { emoji1: '🎉', emoji2: '💸', emoji3: '🎊', position: 'top-center', display: true, layout: 'horizontal' },
        bgGradient: 'linear-gradient(135deg, #e63946 0%, #f77f88 100%)'
      }
    },
    {
      id: 'carousel-with-sidebar',
      type: 'split-layout',
      layout: 'carousel-with-cards',
      splitConfig: { fixedHeight: 450, carouselWidth: 75, cardStackWidth: 25 },
      carousel: {
        items: [
          {
            id: 1,
            badge: { text: 'OCTOBER SPECIAL', icon: '🎃', position: 'top-left', display: true },
            title: { text: 'TREAT YOURSELF', position: 'left-center', display: true, fontSize: 'text-5xl', alignment: 'text-left' },
            subtitle: { text: 'Spooky savings await', position: 'left-center', display: true, fontSize: 'text-lg', alignment: 'text-left' },
            description: { text: 'Halloween collection offers', position: 'left-center', display: true, fontSize: 'text-sm', alignment: 'text-left' },
            primaryButton: { label: 'SHOP NOW', url: '/shop-october', position: 'left-bottom', display: true, style: 'bg-white text-gray-900' },
            secondaryButton: { label: 'View All', url: '/october-deals', position: 'left-bottom', display: true, style: 'border-2 border-white text-white' },
            emoji: { emoji1: '👻', emoji2: '🎃', emoji3: '🦇', position: 'right-center', display: true, layout: 'vertical' },
            bgGradient: 'linear-gradient(135deg, #4a3f8f 0%, #c91e8f 100%)'
          },
          {
            id: 2,
            badge: { text: 'FLASH SALE', icon: '⚡', position: 'top-left', display: true },
            title: { text: 'FLASH SALE', position: 'left-center', display: true, fontSize: 'text-5xl', alignment: 'text-left' },
            subtitle: { text: 'Limited time offers', position: 'left-center', display: true, fontSize: 'text-lg', alignment: 'text-left' },
            description: { text: 'Grab these deals now', position: 'left-center', display: true, fontSize: 'text-sm', alignment: 'text-left' },
            primaryButton: { label: 'SHOP NOW', url: '/shop-flash', position: 'left-bottom', display: true, style: 'bg-white text-gray-900' },
            secondaryButton: { label: 'See Details', url: '/flash-details', position: 'left-bottom', display: true, style: 'border-2 border-white text-white' },
            emoji: { emoji1: '⚡', emoji2: '🔥', emoji3: '💥', position: 'right-center', display: true, layout: 'vertical' },
            bgGradient: 'linear-gradient(135deg, #ff6b6b 0%, #ffa500 100%)'
          }
        ]
      },
      cardStack: {
        items: [
          {
            id: 1,
            badge: { text: 'GIFT CARD', icon: '🎁', position: 'top-center', display: true },
            title: { text: 'BUY VOUCHERS', position: 'center', display: true, fontSize: 'text-sm', alignment: 'text-center' },
            subtitle: { text: 'The perfect gift', position: 'center', display: true, fontSize: 'text-xs', alignment: 'text-center' },
            description: { text: 'Give the gift of choice', position: 'center', display: true, fontSize: 'text-xs', alignment: 'text-center' },
            primaryButton: { label: 'Buy Now', url: '/buy-vouchers', position: 'bottom-center', display: true, style: 'bg-white text-gray-800' },
            secondaryButton: { label: 'Details', url: '/voucher-details', position: 'bottom-center', display: false, style: 'border-white text-white' },
            emoji: { emoji1: '🎉', emoji2: '🎈', emoji3: '🎀', position: 'top-center', display: true, layout: 'horizontal' },
            bgGradient: 'linear-gradient(135deg, #ff9500 0%, #ffb347 100%)'
          },
          {
            id: 2,
            badge: { text: 'BANKING OFFERS', icon: '💳', position: 'top-center', display: true },
            title: { text: 'BANK VOUCHERS', position: 'center', display: true, fontSize: 'text-sm', alignment: 'text-center' },
            subtitle: { text: 'Special offers', position: 'center', display: true, fontSize: 'text-xs', alignment: 'text-center' },
            description: { text: 'Bank partner discounts', position: 'center', display: true, fontSize: 'text-xs', alignment: 'text-center' },
            primaryButton: { label: 'View Offers', url: '/bank-offers', position: 'bottom-center', display: true, style: 'bg-white text-gray-800' },
            secondaryButton: { label: 'Apply Now', url: '/apply-bank-offer', position: 'bottom-center', display: false, style: 'border-white text-white' },
            emoji: { emoji1: '💰', emoji2: '💸', emoji3: '💎', position: 'top-center', display: true, layout: 'horizontal' },
            bgGradient: 'linear-gradient(135deg, #ffb347 0%, #ffa500 100%)'
          }
        ]
      }
    }
  ]
};

const Banner = ({ banner }) => {
  if (banner.layout === 'full-carousel') {
    return <FullCarousel items={banner.items} config={banner.carouselConfig} />;
  }
  if (banner.layout === 'carousel-center-dual-sidebar') {
    return <DualSidebarCarousel carousel={banner.carousel} leftSidebar={banner.leftSidebar} rightSidebar={banner.rightSidebar} config={banner.tripleConfig} />;
  }
  if (banner.layout === 'carousel-with-single-card') {
    return <SingleCardLayout carousel={banner.carousel} singleCard={banner.singleCard} config={banner.splitConfig} />;
  }
  if (banner.layout === 'carousel-with-cards') {
    return <CardStackLayout carousel={banner.carousel} cardStack={banner.cardStack} config={banner.splitConfig} />;
  }
  return null;
};

export default function BannerCMS() {
  const [selectedBanner, setSelectedBanner] = useState(0);

  return (
    <div className="w-[90%] mx-[5%]">
      {/* <div className="bg-white shadow">
        <div className="max-w-6xl mx-auto p-6">
          <h1 className="text-3xl font-bold mb-4">E-Commerce Banner CMS - Configurable JSON</h1>
          <div className="flex gap-2 flex-wrap">
            {bannerData.banners.map((banner, idx) => (
              <button key={banner.id} onClick={() => setSelectedBanner(idx)} className={`px-3 py-2 rounded-lg font-semibold text-xs whitespace-nowrap transition ${selectedBanner === idx ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-800 hover:bg-gray-300'}`}>
                {banner.id.replace(/-/g, ' ').toUpperCase()}
              </button>
            ))}
          </div>
        </div>
      </div> */}

      <div className="bg-white mr-30 ml-30 shadow-lg">
        <Banner banner={bannerData.banners[selectedBanner]} />
      </div>

      {/* <div className="max-w-6xl mx-auto p-6 mt-8">
        <div className="bg-gray-900 text-white p-6 rounded-lg mb-4">
          <h2 className="text-2xl font-bold mb-2">JSON Configuration</h2>
          <pre className="text-xs overflow-auto max-h-96 bg-gray-800 p-4 rounded">
            {JSON.stringify(bannerData.banners[selectedBanner], null, 2)}
          </pre>
        </div>

        <div className="bg-blue-900 text-white p-6 rounded-lg">
          <h2 className="text-xl font-bold mb-3">Configurable Elements</h2>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-blue-800 p-3 rounded">
              <h3 className="font-bold">Badge</h3>
              <p className="text-xs text-gray-300">text, icon, position, display</p>
            </div>
            <div className="bg-blue-800 p-3 rounded">
              <h3 className="font-bold">Title/Subtitle</h3>
              <p className="text-xs text-gray-300">fontSize, alignment, position, display</p>
            </div>
            <div className="bg-blue-800 p-3 rounded">
              <h3 className="font-bold">Buttons</h3>
              <p className="text-xs text-gray-300">label, url, style, display</p>
            </div>
            <div className="bg-blue-800 p-3 rounded">
              <h3 className="font-bold">Emojis</h3>
              <p className="text-xs text-gray-300">position, layout, display</p>
            </div>
          </div>
        </div>
      </div> */}
    </div>
  );
}