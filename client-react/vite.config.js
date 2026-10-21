import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: './',
  server: {
    host: '127.0.0.1',
    port: 5500,
    strictPort: true,
    proxy: {
      '/api/city': {
        target: 'https://localhost:8181',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api\/city/, '/city-service')
      },
      '/api/route': {
        target: 'https://localhost:8443',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api\/route/, '/route-service')
      }
    }
  },
  build: {
    target: 'es2020'
  }
});
