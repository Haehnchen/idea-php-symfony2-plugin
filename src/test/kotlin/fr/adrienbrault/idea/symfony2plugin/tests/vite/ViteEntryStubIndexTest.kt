package fr.adrienbrault.idea.symfony2plugin.tests.vite

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.vite.VITE_ENTRY_STUB_INDEX_KEY
import fr.adrienbrault.idea.symfony2plugin.vite.ViteEntryStubIndex

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteEntryStubIndex
 */
class ViteEntryStubIndexTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testSimpleEntryIsIndexed() {
        myFixture.addFileToProject(
            "vite.config.js",
            """
            import { defineConfig } from 'vite';
            export default defineConfig({
                build: {
                    rollupOptions: {
                        input: {
                            app: './assets/app.ts'
                        }
                    }
                }
            });
            """.trimIndent()
        )

        assertIndexContainsKeyWithValue(VITE_ENTRY_STUB_INDEX_KEY, "assets/app.ts", "app")
    }

    fun testMultipleEntriesAreIndexed() {
        myFixture.addFileToProject(
            "vite.config.js",
            """
            import { defineConfig } from 'vite';
            export default defineConfig({
                build: {
                    rollupOptions: {
                        input: {
                            app:   './assets/app.ts',
                            admin: './assets/admin.js'
                        }
                    }
                }
            });
            """.trimIndent()
        )

        assertIndexContainsKeyWithValue(VITE_ENTRY_STUB_INDEX_KEY, "assets/app.ts", "app")
        assertIndexContainsKeyWithValue(VITE_ENTRY_STUB_INDEX_KEY, "assets/admin.js", "admin")
    }

    fun testPathWithoutLeadingDotSlashIsNormalized() {
        myFixture.addFileToProject(
            "vite.config.js",
            """
            import { defineConfig } from 'vite';
            export default defineConfig({
                build: {
                    rollupOptions: {
                        input: {
                            app: 'assets/app.ts'
                        }
                    }
                }
            });
            """.trimIndent()
        )

        assertIndexContainsKeyWithValue(VITE_ENTRY_STUB_INDEX_KEY, "assets/app.ts", "app")
    }

    fun testTypeScriptConfigIsIndexed() {
        myFixture.addFileToProject(
            "vite.config.ts",
            """
            import { defineConfig } from 'vite';
            export default defineConfig({
                build: {
                    rollupOptions: {
                        input: {
                            main: './assets/main.ts'
                        }
                    }
                }
            });
            """.trimIndent()
        )

        assertIndexContainsKeyWithValue(VITE_ENTRY_STUB_INDEX_KEY, "assets/main.ts", "main")
    }

    fun testSpreadEntriesAreIndexed() {
        myFixture.addFileToProject(
            "vite.config.ts",
            """
            import { defineConfig } from 'vite';
            const extras = {
                extra: './assets/extra.js'
            };
            export default defineConfig({
                build: {
                    rollupOptions: {
                        input: {
                            ...extras,
                            app: './assets/app.ts'
                        }
                    }
                }
            });
            """.trimIndent()
        )

        assertIndexContainsKeyWithValue(VITE_ENTRY_STUB_INDEX_KEY, "assets/app.ts", "app")
        assertIndexContainsKeyWithValue(VITE_ENTRY_STUB_INDEX_KEY, "assets/extra.js", "extra")
    }
}
