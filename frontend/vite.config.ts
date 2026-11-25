import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // The app calls relative /api/... URLs. In dev, Vite forwards them to Spring Boot,
    // so the browser sees one origin (no CORS) and no API URL is baked into the code.
    // In Docker, nginx does the same job.
    proxy: { '/api': 'http://localhost:8080' },
  },
})
