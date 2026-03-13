import { defineConfig } from 'vite';

const entries = {
    app: './assets/app.js',
    admin: './assets/admin.js'
};

export default defineConfig({
    build: {
        rollupOptions: {
            input: entries
        }
    }
});
