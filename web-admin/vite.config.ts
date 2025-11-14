import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_');
  const proxyTarget = env.VITE_PROXY_TARGET;
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: proxyTarget
        ? {
            '/vendors': { target: proxyTarget, changeOrigin: true },
            '/products': { target: proxyTarget, changeOrigin: true },
            '/customers': { target: proxyTarget, changeOrigin: true },
            '/cuisines': { target: proxyTarget, changeOrigin: true },
          }
        : undefined,
    },
  };
});

