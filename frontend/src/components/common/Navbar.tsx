import React from 'react';
import { Link } from 'react-router-dom';
import { Link2 } from 'lucide-react';

const Navbar: React.FC = () => {
  return (
    <nav className="bg-white border-b border-gray-200 px-4 py-3">
      <div className="max-w-7xl mx-auto flex justify-between items-center">
        <Link to="/" className="flex items-center gap-2 text-xl font-bold text-indigo-600 hover:text-indigo-700 transition-colors">
          <Link2 className="w-6 h-6" />
          <span>URL Shortener</span>
        </Link>
        <div className="flex gap-6 text-sm font-medium text-gray-600">
          <Link to="/" className="hover:text-indigo-600 transition-colors">Home</Link>
          <Link to="/manage" className="hover:text-indigo-600 transition-colors">Manage</Link>
          <Link to="/analytics" className="hover:text-indigo-600 transition-colors">Analytics</Link>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
