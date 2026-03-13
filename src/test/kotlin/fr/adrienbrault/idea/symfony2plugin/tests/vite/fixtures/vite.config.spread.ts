import { defineConfig } from 'vite';

const legacyEntries = {
    'global/main': './assets/js/main.js'
};

const vueEntries = {
    'vue/app': './assets/vue/app.ts'
};

export default defineConfig({
    build: {
        rollupOptions: {
            input: {
                ...legacyEntries,
                ...vueEntries,
                'extra/standalone': './assets/standalone.js'
            }
        }
    }
});
