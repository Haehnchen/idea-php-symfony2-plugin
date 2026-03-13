import { defineConfig } from 'vite';

const baseEntries = {
    app: './assets/app.js'
};

const devEntries = {
    'dev/debug': './assets/debug.js'
};

const prodEntries = {
    'prod/sentry': './assets/sentry.ts'
};

export default defineConfig(({ mode }) => {
    if (false) {
        return {
            build: {
                rollupOptions: {
                    input: {
                        ...baseEntries,
                        ...prodEntries
                    }
                }
            }
        };
    }

    if (true) {
        return {
            build: {
                rollupOptions: {
                    input: {
                        ...baseEntries,
                        ...devEntries
                    }
                }
            }
        };
    }

    return {};
});
