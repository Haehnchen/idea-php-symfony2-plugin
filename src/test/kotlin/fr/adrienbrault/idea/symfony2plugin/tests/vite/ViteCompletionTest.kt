package fr.adrienbrault.idea.symfony2plugin.tests.vite

import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.vite.ViteGotoCompletionRegistrar

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteGotoCompletionRegistrar
 */
class ViteCompletionTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testCompletionInViteEntryLinkTags() {
        myFixture.addFileToProject(
            "vite.config.js",
            """
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
            """.trimIndent()
        )

        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "app")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "admin")
    }

    fun testCompletionInViteEntryScriptTags() {
        myFixture.addFileToProject(
            "vite.config.js",
            """
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
            """.trimIndent()
        )

        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_script_tags('<caret>') }}", "app")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_script_tags('<caret>') }}", "admin")
    }

    fun testCompletionFromTypeScriptConfig() {
        myFixture.addFileToProject(
            "vite.config.ts",
            """
            import { defineConfig } from 'vite';
            export default defineConfig({
                build: {
                    rollupOptions: {
                        input: {
                            main: './assets/main.ts',
                            styles: './assets/styles.css'
                        }
                    }
                }
            });
            """.trimIndent()
        )

        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "main")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "styles")
    }

    fun testCompletionWithVariableReference() {
        myFixture.addFileToProject(
            "vite.config.ts",
            """
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
            """.trimIndent()
        )

        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "app")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "admin")
    }

    fun testCompletionWithSpreadOperator() {
        myFixture.addFileToProject(
            "vite.config.ts",
            """
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
            """.trimIndent()
        )

        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "global/main")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "vue/app")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('<caret>') }}", "extra/standalone")
    }

    fun testNavigationToConfigFile() {
        myFixture.addFileToProject(
            "vite.config.js",
            """
            import { defineConfig } from 'vite';
            export default defineConfig({
                build: {
                    rollupOptions: {
                        input: {
                            app: './assets/app.js'
                        }
                    }
                }
            });
            """.trimIndent()
        )

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ vite_entry_link_tags('ap<caret>p') }}")
    }
}
