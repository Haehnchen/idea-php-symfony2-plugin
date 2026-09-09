package fr.adrienbrault.idea.symfony2plugin.tests.vite

import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.patterns.PlatformPatterns
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.vite.ViteGotoCompletionRegistrar

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteGotoCompletionRegistrar
 */
class ViteGotoCompletionRegistrarTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testUnrelatedInputObjectsAreExcluded() {
        myFixture.addFileToProject("vite.config.ts", """
            const unused = { input: { unused: './unused.js' } };
            export default defineConfig({
                input: { app: './app.js' },
                plugins: [plugin({ input: { pluginOption: './plugin.js' } })],
                custom: { build: { rolldownOptions: { input: { custom: './custom.js' } } } }
            });
        """.trimIndent())

        for (function in listOf("vite_entry_script_tags", "reprise_entry_script_tags")) {
            assertCompletionContains(TwigFileType.INSTANCE, "{{ $function('<caret>') }}", "app")
            assertCompletionNotContains(TwigFileType.INSTANCE, "{{ $function('<caret>') }}", "unused", "pluginOption", "custom")
            assertNavigationIsEmpty(TwigFileType.INSTANCE, "{{ $function('un<caret>used') }}")
        }
    }

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

    private fun addRepriseConfig() {
        myFixture.addFileToProject("assets/checkout.ts", "export const ready = true;")
        myFixture.addFileToProject("vite.config.ts", """
            import { defineConfig } from 'vite';
            export default defineConfig({
                input: { checkout: './assets/checkout.ts', 'page/home': './assets/missing.ts' }
            });
        """.trimIndent())
    }

    fun testRepriseFunctionsAndTwigContexts() {
        addRepriseConfig()

        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_script_tags('<caret>') }}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% if reprise_entry_script_tags(\"<caret>\") %}{% endif %}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% set entries = reprise_entry_script_tags('<caret>') %}", "checkout", "page/home")

        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags('<caret>') }}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% if reprise_entry_link_tags(\"<caret>\") %}{% endif %}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% set entries = reprise_entry_link_tags('<caret>') %}", "checkout", "page/home")

        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_js_files('<caret>') }}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% if reprise_entry_js_files(\"<caret>\") %}{% endif %}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% set entries = reprise_entry_js_files('<caret>') %}", "checkout", "page/home")

        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_css_files('<caret>') }}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% if reprise_entry_css_files(\"<caret>\") %}{% endif %}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% set entries = reprise_entry_css_files('<caret>') %}", "checkout", "page/home")

        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_exists('<caret>') }}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% if reprise_entry_exists(\"<caret>\") %}{% endif %}", "checkout", "page/home")
        assertCompletionContains(TwigFileType.INSTANCE, "{% set entries = reprise_entry_exists('<caret>') %}", "checkout", "page/home")
    }

    fun testRepriseNamedEntryAndDefaultBuild() {
        addRepriseConfig()
        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags(entryName='<caret>') }}", "checkout")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags(build=null, entryName='<caret>') }}", "checkout")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags(packageName='cdn', entryName='<caret>', build=null) }}", "checkout")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags(entryName: '<caret>', build: null) }}", "checkout")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags('<caret>', 'cdn', null, {'class': 'foo,bar'}) }}", "checkout")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_exists('<caret>', null) }}", "checkout")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_js_files('page/<caret>') }}", "page/home")
    }

    fun testRepriseNavigationToSourceAndConfig() {
        addRepriseConfig()
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ reprise_entry_script_tags('check<caret>out') }}", "assets/checkout.ts")
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags(entryName='page/ho<caret>me') }}",
            PlatformPatterns.psiElement(JSProperty::class.java).withText("'page/home': './assets/missing.ts'"))
    }

    fun testRepriseCompletionAndNavigationFromRollupInput() {
        assertNestedInputCompletionAndNavigation("rollupOptions")
    }

    fun testRepriseCompletionAndNavigationFromRolldownInput() {
        assertNestedInputCompletionAndNavigation("rolldownOptions")
    }

    private fun assertNestedInputCompletionAndNavigation(options: String) {
        myFixture.addFileToProject("assets/nested.js", "export const ready = true;")
        myFixture.addFileToProject("vite.config.js", """
            import { defineConfig } from 'vite';
            export default defineConfig({
                build: { $options: { input: { nested: './assets/nested.js' } } }
            });
        """.trimIndent())

        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_script_tags('<caret>') }}", "nested")
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ reprise_entry_script_tags('nes<caret>ted') }}", "assets/nested.js")
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ reprise_entry_script_tags('nes<caret>ted') }}",
            PlatformPatterns.psiElement(JSProperty::class.java).withText("nested: './assets/nested.js'"))
    }

    fun testRepriseCompletionAndNavigationFromLocalInputVariable() {
        myFixture.addFileToProject("assets/local.js", "export const ready = true;")
        myFixture.addFileToProject("vite.config.js", """
            const entries = { local: './assets/local.js' };
            export default { input: entries };
        """.trimIndent())

        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_js_files(entryName='<caret>') }}", "local")
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ reprise_entry_js_files(entryName='lo<caret>cal') }}", "assets/local.js")
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ reprise_entry_js_files(entryName='lo<caret>cal') }}",
            PlatformPatterns.psiElement(JSProperty::class.java).withText("local: './assets/local.js'"))
    }

    fun testRepriseCompletionAndNavigationFromSpreadEntries() {
        myFixture.addFileToProject("assets/spread.js", "export const ready = true;")
        myFixture.addFileToProject("vite.config.js", """
            const shared = { 'shared/app': './assets/spread.js' };
            const entries = { ...shared, styles: './assets/missing.css' };
            export default { input: { ...entries } };
        """.trimIndent())

        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags('<caret>') }}", "shared/app", "styles")
        assertCompletionContains(TwigFileType.INSTANCE, "{{ reprise_entry_js_files('shared/<caret>') }}", "shared/app")
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{{ reprise_entry_js_files('shared/ap<caret>p') }}", "assets/spread.js")
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ reprise_entry_js_files('shared/ap<caret>p') }}",
            PlatformPatterns.psiElement(JSProperty::class.java).withText("'shared/app': './assets/spread.js'"))
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ reprise_entry_css_files('sty<caret>les') }}",
            PlatformPatterns.psiElement(JSProperty::class.java).withText("styles: './assets/missing.css'"))
    }

    fun testRepriseOtherArgumentsAndUnsupportedBuildsAreExcluded() {
        addRepriseConfig()
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_script_tags('checkout', '<caret>') }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_script_tags('checkout', build='<caret>') }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags(packageName='<caret>') }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags('checkout', attributes={'class': '<caret>'}) }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_script_tags('<caret>', null, 'widget') }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_exists('<caret>', 'widget') }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags(entryName='<caret>', build=selectedBuild) }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags(build='widget', entryName='<caret>') }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ REPRISE_ENTRY_LINK_TAGS('<caret>') }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ unrelated('<caret>') }}", "checkout")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ reprise_entry_link_tags('<caret>' ~ suffix) }}", "checkout")
        assertNavigationIsEmpty(TwigFileType.INSTANCE, "{{ reprise_entry_exists('check<caret>out', 'widget') }}")
        assertCompletionNotContains(TwigFileType.INSTANCE, "{# reprise_entry_link_tags('<caret>') #}", "checkout")
    }
}
