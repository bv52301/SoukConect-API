import { useEffect, useMemo, useState } from 'react';
import { Dialog, DialogTitle, DialogContent, IconButton, Stack, Tooltip, Typography } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import DeleteIcon from '@mui/icons-material/Delete';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';

export type PreviewItem = {
  url: string;
  type?: 'image' | 'video';
  title?: string;
  mimeType?: string;
  sizeBytes?: number;
  downloadUrl?: string; // if different from url
  onDelete?: () => Promise<void> | void; // optional delete action
};

function guessType(url: string): 'image' | 'video' {
  const u = url.toLowerCase();
  if (/(\.mp4|\.webm|\.ogg|\.mov|\.mkv|\.m3u8)(\?|#|$)/.test(u)) return 'video';
  return 'image';
}

export default function PreviewGallery({ open, onClose, items, startIndex = 0 }: { open: boolean; onClose: () => void; items: PreviewItem[]; startIndex?: number }) {
  const [idx, setIdx] = useState(startIndex);
  const [localItems, setLocalItems] = useState<PreviewItem[]>(items);
  const total = localItems.length;
  const current = useMemo(() => localItems.length ? localItems[(idx + total) % total] : undefined, [localItems, idx, total]);

  useEffect(() => { if (open) { setIdx(startIndex); setLocalItems(items); } }, [open, startIndex, items]);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (!open) return;
      if (e.key === 'ArrowRight') setIdx(i => (i + 1) % total);
      if (e.key === 'ArrowLeft') setIdx(i => (i - 1 + total) % total);
      if (e.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, total, onClose]);

  if (!open) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span>Preview {total > 1 ? `${idx + 1} / ${total}` : ''}</span>
        <span>
          {current && (
            <>
              <Tooltip title="Open in new tab">
                <IconButton onClick={() => window.open((current.downloadUrl || current.url) + `?t=${Date.now()}`, '_blank')} size="small"><OpenInNewIcon /></IconButton>
              </Tooltip>
              <Tooltip title="Copy link">
                <IconButton onClick={async () => { try { await navigator.clipboard.writeText(current.downloadUrl || current.url); } catch {} }} size="small"><ContentCopyIcon /></IconButton>
              </Tooltip>
              {current.onDelete && (
                <Tooltip title="Delete">
                  <IconButton
                    onClick={async () => {
                      try {
                        await current.onDelete!();
                        setLocalItems(prev => {
                          const copy = [...prev];
                          copy.splice(idx, 1);
                          return copy;
                        });
                        setIdx(i => Math.max(0, Math.min(i, total - 2)));
                      } catch {}
                    }} size="small"><DeleteIcon /></IconButton>
                </Tooltip>
              )}
            </>
          )}
          <IconButton onClick={onClose}><CloseIcon /></IconButton>
        </span>
      </DialogTitle>
      <DialogContent>
        <Stack direction="row" spacing={1} alignItems="center" justifyContent="center">
          {total > 1 && (<IconButton onClick={() => setIdx(i => (i - 1 + total) % total)} aria-label="Previous"><ChevronLeftIcon /></IconButton>)}
          <div style={{ maxHeight: 480, maxWidth: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {current && ((current.type || guessType(current.url)) === 'video' ? (
              <video src={current.url + `?t=${Date.now()}`} controls style={{ maxHeight: 460, maxWidth: '100%' }} />
            ) : (
              <img src={current.url + `?t=${Date.now()}`} alt={current?.title || ''} style={{ maxHeight: 460, maxWidth: '100%' }} />
            ))}
          </div>
          {total > 1 && (<IconButton onClick={() => setIdx(i => (i + 1) % total)} aria-label="Next"><ChevronRightIcon /></IconButton>)}
        </Stack>
        {current && (
          <Typography variant="caption" sx={{ mt: 1, display: 'block', textAlign: 'center', color: 'text.secondary' }}>
            {(current.mimeType ? `${current.mimeType}` : '')}{current.mimeType && current.sizeBytes ? ' • ' : ''}{current.sizeBytes ? humanSize(current.sizeBytes) : ''}
          </Typography>
        )}
        {current && (
          <Typography variant="caption" sx={{ mt: 0.5, display: 'block', textAlign: 'center', color: 'text.secondary' }}>
            {fileNameFromUrl(current.downloadUrl || current.url)}
          </Typography>
        )}
      </DialogContent>
    </Dialog>
  );
}

function humanSize(b?: number) {
  if (!b || b <= 0) return '';
  const units = ['B','KB','MB','GB'];
  let i = 0; let n = b;
  while (n >= 1024 && i < units.length-1) { n/=1024; i++; }
  return `${n.toFixed(1)} ${units[i]}`;
}

function fileNameFromUrl(u: string) {
  try {
    const noQuery = u.split('?')[0];
    return decodeURIComponent(noQuery.substring(noQuery.lastIndexOf('/') + 1));
  } catch {
    return u;
  }
}
