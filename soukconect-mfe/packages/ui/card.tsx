interface CardProps {
  children: React.ReactNode;
  className?: string;
  hover?: boolean;
}

export function Card({ children, className = '', hover = false }: CardProps) {
  return (
    <div
      className={`bg-white rounded-xl shadow-md ${
        hover ? 'hover:shadow-lg transition-shadow' : ''
      } ${className}`}
    >
      {children}
    </div>
  );
}