import React from 'react';

const Footer: React.FC = () => {
  return (
    <footer className="mt-auto border-t border-slate-200 bg-white py-6">
      <div className="mx-auto flex max-w-6xl flex-col gap-2 px-4 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
        <p>&copy; {new Date().getFullYear()} Shortstack engineering prototype</p>
        <p>Live values come from the configured API. Architecture content is explanatory.</p>
      </div>
    </footer>
  );
};

export default Footer;
