import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/common/Navbar';
import Footer from './components/common/Footer';
import Home from './pages/Home';
import Manage from './pages/Manage';
import Analytics from './pages/Analytics';

const App: React.FC = () => {
  return (
    <Router>
      <div className="flex flex-col min-h-screen bg-white">
        <Navbar />
        <main className="flex-grow">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/manage/:code" element={<Manage />} />
            <Route path="/analytics/:code" element={<Analytics />} />
            {/* Fallback for root manage/analytics to redirect or show error */}
            <Route path="/manage" element={<div className="py-20 text-center text-gray-500">Please provide a link code in the URL.</div>} />
            <Route path="/analytics" element={<div className="py-20 text-center text-gray-500">Please provide a link code in the URL.</div>} />
          </Routes>
        </main>
        <Footer />
      </div>
    </Router>
  );
};

export default App;
