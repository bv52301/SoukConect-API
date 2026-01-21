import React, { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export const CardStackLayout = ({ carousel, cardStack, config }) => {
  const [idx, setIdx] = useState(0);
  const item = carousel.items[idx];
  const cardHeight = config.fixedHeight / cardStack.items.length;
  return (
    <div className="flex w-full" style={{ minHeight: config.fixedHeight }}>
      <div className="relative overflow-hidden flex items-center justify-center" style={{ width: config.carouselWidth + '%', background: item.bgGradient, minHeight: config.fixedHeight }}>
        <div className="px-12 z-10 text-center md:text-left w-full">
          {item.badge.display && <div className="inline-flex items-center gap-2 mb-4 bg-white bg-opacity-20 px-4 py-2 rounded-full"><span className="text-xl">{item.badge.icon}</span><span className="text-sm font-bold">{item.badge.text}</span></div>}
          {item.title.display && <h1 className={`${item.title.fontSize} font-bold mb-4`} style={{ color: '#fff' }}>{item.title.text}</h1>}
          {item.subtitle.display && <p className={`${item.subtitle.fontSize} font-semibold mb-3`} style={{ color: '#fff' }}>{item.subtitle.text}</p>}
          {item.description.display && <p className={`${item.description.fontSize} mb-8 opacity-90`} style={{ color: '#fff' }}>{item.description.text}</p>}
          <div className="flex gap-4">
            {item.primaryButton.display && <a href={item.primaryButton.url} className={`font-bold px-8 py-3 rounded-lg hover:opacity-80 inline-block ${item.primaryButton.style}`}>{item.primaryButton.label}</a>}
            {item.secondaryButton.display && <a href={item.secondaryButton.url} className={`font-bold px-8 py-3 rounded-lg hover:opacity-80 inline-block ${item.secondaryButton.style}`}>{item.secondaryButton.label}</a>}
          </div>
        </div>
        {item.emoji.display && <div className="absolute right-8 top-1/2 -translate-y-1/2 flex gap-4 text-7xl opacity-80"><div>{item.emoji.emoji1}</div><div className="opacity-60">{item.emoji.emoji2}</div></div>}
        <button onClick={() => setIdx((p) => (p - 1 + carousel.items.length) % carousel.items.length)} className="absolute left-4 top-1/2 -translate-y-1/2 bg-white bg-opacity-50 p-2 rounded-full z-20"><ChevronLeft size={28} /></button>
        <button onClick={() => setIdx((p) => (p + 1) % carousel.items.length)} className="absolute right-4 top-1/2 -translate-y-1/2 bg-white bg-opacity-50 p-2 rounded-full z-20"><ChevronRight size={28} /></button>
      </div>
      <div className="flex flex-col" style={{ width: config.cardStackWidth + '%', minHeight: config.fixedHeight }}>
        {cardStack.items.map((card, cidx) => (
          <div key={card.id} className="flex-1 p-4 text-white flex flex-col justify-between" style={{ background: card.bgGradient, borderBottom: cidx < cardStack.items.length - 1 ? '2px solid rgba(0,0,0,0.15)' : 'none', minHeight: cardHeight }}>
            <div>
              {card.badge.display && <div className="inline-flex items-center gap-2 mb-2 bg-white bg-opacity-20 px-2 py-1 rounded-full text-xs"><span>{card.badge.icon}</span><span className="font-bold">{card.badge.text}</span></div>}
              {card.emoji.display && <div className="text-3xl mb-2">{card.emoji.emoji1}</div>}
              {card.title.display && <h3 className={`${card.title.fontSize} font-bold mb-1`}>{card.title.text}</h3>}
              {card.subtitle.display && <p className={`${card.subtitle.fontSize} opacity-90`}>{card.subtitle.text}</p>}
            </div>
            {card.primaryButton.display && <a href={card.primaryButton.url} className={`font-bold px-3 py-2 rounded w-full hover:opacity-80 transition text-xs mt-2 inline-block text-center ${card.primaryButton.style}`}>{card.primaryButton.label}</a>}
          </div>
        ))}
      </div>
    </div>
  );
};