import { getVendor, getAllVendorIds, getVendorDetails, getVendorProducts } from '../../../lib/data';
import { notFound } from 'next/navigation';
import CartWrapper from '@/components/cart-wrapper';

// ISR - Revalidate every 5 minutes
export const revalidate = 300;

// Generate static paths for all chefs
export async function generateStaticParams() {
  const chefIds = await getAllVendorIds();
  return chefIds.map((id) => ({ id }));
}

// Generate metadata for SEO
export async function generateMetadata({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params; // Await params first
  const chef = await getVendor(id);
  
  if (!chef) {
    return { title: 'Chef Not Found' };
  }

  return {
    title: `${chef.name} - Food Connect`,
    description: chef.description,
  };
}

interface VendorPageProps {
  params: { id: string };
}

export default async function VendorPage({ params }: VendorPageProps) {
  const vendorId = parseInt(params.id, 10);
  if (Number.isNaN(vendorId)) {
    notFound();
  }

  const chef = await getVendorDetails(vendorId);
  if (!chef) {
    notFound();
  }

  const sampleProducts = await getVendorProducts(vendorId);

  const bgColors = [
    'bg-gradient-to-br from-orange-400 to-orange-500',
    'bg-gradient-to-br from-teal-400 to-teal-500',
    'bg-gradient-to-br from-green-400 to-green-500',
    'bg-gradient-to-br from-purple-400 to-purple-500',
  ];

  const randomBg = bgColors[Number(chef.vendorId) % bgColors.length];

  // Pass server-fetched data to client component
  return (
    <CartWrapper 
      chef={chef} 
      sampleProducts={sampleProducts ?? []} 
      randomBg={randomBg} 
    />
  );
}