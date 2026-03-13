import { defineConfig } from 'vite';

export default defineConfig({
    build: {
        rollupOptions: {
            input: {
                app: './assets/app.js',
                admin: './assets/admin.js'
            }
        }
    }
});
