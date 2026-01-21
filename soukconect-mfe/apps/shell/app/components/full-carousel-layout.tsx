import React, { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export const FullCarousel = ({ items, config }) => {
  const [idx, setIdx] = useState(0);
  const item = items[idx];
  return (
    <div className="relative w-full overflow-hidden" style={{ minHeight: config.fixedHeight }}>
      <div className="w-full flex items-center justify-center px-12" style={{ background: item.bgGradient, minHeight: config.fixedHeight, color: '#fff' }}>
        <div className="max-w-6xl mx-auto w-full flex items-center justify-between gap-12">
          <div className="flex-1">
            {item.badge.display && (
              <div className="inline-flex items-center gap-2 mb-4 bg-white bg-opacity-20 px-4 py-2 rounded-full">
                <span className="text-xl">{item.badge.icon}</span>
                <span className="text-sm font-bold">{item.badge.text}</span>
              </div>
            )}
            {item.title.display && (
              <h1 className={`${item.title.fontSize} font-bold mb-4 ${item.title.alignment}`}>{item.title.text}</h1>
            )}
            {item.subtitle.display && (
              <p className={`${item.subtitle.fontSize} font-semibold mb-3 ${item.subtitle.alignment}`}>{item.subtitle.text}</p>
            )}
            {item.description.display && (
              <p className={`${item.description.fontSize} mb-8 opacity-90 ${item.description.alignment}`}>{item.description.text}</p>
            )}
            <div className="flex gap-4">
              {item.primaryButton.display && (
                <a href={item.primaryButton.url} className={`font-bold px-12 py-4 rounded-lg hover:opacity-80 ${item.primaryButton.style}`}>
                  {item.primaryButton.label}
                </a>
              )}
              {item.secondaryButton.display && (
                <a href={item.secondaryButton.url} className={`font-bold px-12 py-4 rounded-lg hover:opacity-80 ${item.secondaryButton.style}`}>
                  {item.secondaryButton.label}
                </a>
              )}
            </div>
          </div>
          {item.emoji.display && (
            <div className="flex-1 text-center">
              <div className="text-9xl">{item.emoji.emoji1}</div>
              <div className="text-7xl opacity-60 ml-20">{item.emoji.emoji2}</div>
              <div className="text-7xl opacity-40 ml-40">{item.emoji.emoji3}</div>
            </div>
          )}
        </div>
      </div>
      <button onClick={() => setIdx((p) => (p - 1 + items.length) % items.length)} className="absolute left-4 top-1/2 -translate-y-1/2 bg-white bg-opacity-50 hover:bg-opacity-75 p-3 rounded-full z-20">
        <ChevronLeft size={32} />
      </button>
      <button onClick={() => setIdx((p) => (p + 1) % items.length)} className="absolute right-4 top-1/2 -translate-y-1/2 bg-white bg-opacity-50 hover:bg-opacity-75 p-3 rounded-full z-20">
        <ChevronRight size={32} />
      </button>
      <div className="flex justify-center gap-3 absolute bottom-8 left-0 right-0 z-20">
        {items.map((_, i) => (
          <button key={i} onClick={() => setIdx(i)} className={`transition ${i === idx ? 'w-10 h-3 bg-white rounded-full' : 'w-3 h-3 bg-white bg-opacity-50 rounded-full'}`} />
        ))}
      </div>
    </div>
  );
};