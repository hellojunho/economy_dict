import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const defaultFrontendPort = 5555;
const frontendPort = Number.parseInt(process.env.FRONTEND_PORT ?? String(defaultFrontendPort), 10);
const resolvedFrontendPort = Number.isNaN(frontendPort) ? defaultFrontendPort : frontendPort;
const apiTarget = process.env.VITE_API_TARGET ?? 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  server: {
    port: resolvedFrontendPort,
    strictPort: true,
    host: true,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  }
});
