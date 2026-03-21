import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const frontendPort = Number.parseInt(process.env.FRONTEND_PORT ?? '4321', 10);
const apiTarget = process.env.VITE_API_TARGET ?? 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  server: {
    port: Number.isNaN(frontendPort) ? 4321 : frontendPort,
    host: true,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  }
});
