import React from 'react';
import CreateLinkForm from '../components/features/CreateLinkForm';
import { Zap, Shield, BarChart3 } from 'lucide-react';

const Home: React.FC = () => {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="flex-grow">
        {/* Hero Section */}
        <section className="bg-gradient-to-b from-indigo-50 to-white py-20 px-4 text-center">
          <div className="max-w-4xl mx-auto">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-100 text-indigo-600 text-xs font-bold uppercase tracking-wider mb-6">
              <Zap className="w-3 h-3" />
              <span>Fast & Simple</span>
            </div>
            <h1 className="text-5xl md:text-6xl font-extrabold text-gray-900 mb-6 tracking-tight">
              Shorten your links, <span className="text-indigo-600">expand your reach.</span>
            </h1>
            <p className="text-lg text-gray-600 mb-12 max-w-2xl mx-auto leading-relaxed">
              Create short, shareable links in seconds. Track performance and manage your links with ease.
            </p>
            <CreateLinkForm />
          </div>
        </section>

        {/* Features Section */}
        <section className="py-20 px-4 bg-white">
          <div className="max-w-7xl mx-auto">
            <div className="text-center mb-16">
              <h2 className="text-3xl font-bold text-gray-900 mb-4">Why use our URL Shortener?</h2>
              <p className="text-gray-600 max-w-2xl mx-auto">Everything you need to manage your links effectively in one place.</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div className="p-8 rounded-2xl border border-gray-100 bg-gray-50 hover:border-indigo-200 transition-colors group">
                <div className="w-12 h-12 bg-indigo-100 rounded-xl flex items-center justify-center mb-6 group-hover:bg-indigo-600 transition-colors">
                  <Zap className="w-6 h-6 text-indigo-600 group-hover:text-white transition-colors" />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-3">Instant Shortening</h3>
                <p className="text-gray-600 leading-relaxed">
                  Convert long, cumbersome URLs into clean, short links instantly. Perfect for social media and emails.
                </p>
              </div>
              <div className="p-8 rounded-2xl border border-gray-100 bg-gray-50 hover:border-indigo-200 transition-colors group">
                <div className="w-12 h-12 bg-indigo-100 rounded-xl flex items-center justify-center mb-6 group-hover:bg-indigo-600 transition-colors">
                  <Shield className="w-6 h-6 text-indigo-600 group-hover:text-white transition-colors" />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-3">Secure Management</h3>
                <p className="text-gray-600 leading-relaxed">
                  Your links are protected by owner tokens. Only those with the token can update or disable a link.
                </p>
              </div>
              <div className="p-8 rounded-2xl border border-gray-100 bg-gray-50 hover:border-indigo-200 transition-colors group">
                <div className="w-12 h-12 bg-indigo-100 rounded-xl flex items-center justify-center mb-6 group-hover:bg-indigo-600 transition-colors">
                  <BarChart3 className="w-6 h-6 text-indigo-600 group-hover:text-white transition-colors" />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-3">Click Analytics</h3>
                <p className="text-gray-600 leading-relaxed">
                  Track how many times your links are clicked. Get real-time totals to measure your impact.
                </p>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default Home;
