export default function NotFound() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-12 px-4">
      <div className="text-center">
        <h2 className="text-3xl font-bold mb-4">Chef Not Found</h2>
        <p className="text-gray-600 mb-6">
          The chef you're looking for doesn't exist.
        </p>
        <a
          href="/"
          className="inline-block bg-blue-600 hover:bg-blue-700 text-white font-semibold px-8 py-3 rounded-lg transition"
        >
          View All Chefs
        </a>
      </div>
    </div>
  );
}