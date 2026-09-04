import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/common/Navbar';
import Footer from './components/common/Footer';
import Home from './pages/Home';
import Manage from './pages/Manage';
import Analytics from './pages/Analytics';
import Architecture from './pages/Architecture';
import EngineeringReview from './pages/EngineeringReview';

const App: React.FC = () => {
  return (
    <Router>
      <div className="flex min-h-screen flex-col bg-[#fbfcfe] text-slate-900">
        <Navbar />
        <main className="flex-grow">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/manage/:code" element={<Manage />} />
            <Route path="/analytics/:code" element={<Analytics />} />
            <Route path="/manage" element={<Manage />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/architecture" element={<Architecture />} />
            <Route path="/engineering-review" element={<EngineeringReview />} />
            <Route path="*" element={<Home />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </Router>
  );
};

export default App;
