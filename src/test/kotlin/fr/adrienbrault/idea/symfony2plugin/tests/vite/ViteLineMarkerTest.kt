package fr.adrienbrault.idea.symfony2plugin.tests.vite

import com.jetbrains.twig.TwigFileType
import com.jetbrains.twig.elements.TwigElementTypes
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.vite.ViteJavaScriptLineMarkerProvider

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteJavaScriptLineMarkerProvider
 */
class ViteLineMarkerTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testLineMarkerOnJavaScriptEntryFile() {
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

        val appFile = myFixture.addFileToProject(
            "assets/app.js",
            "console.log('hello');"
        )

        assertLineMarker(appFile, LineMarker.ToolTipEqualsAssert("Vite entry point"))
    }

    fun testLineMarkerOnTypeScriptEntryFile() {
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

        val mainFile = myFixture.addFileToProject(
            "assets/main.ts",
            "export const x = 1;"
        )

        assertLineMarker(mainFile, LineMarker.ToolTipEqualsAssert("Vite entry point"))
    }

    fun testNoLineMarkerForNonEntryFile() {
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

        val otherFile = myFixture.addFileToProject(
            "assets/other.js",
            "console.log('not an entry');"
        )

        assertLineMarkerIsEmpty(otherFile)
    }

    fun testLineMarkerNavigatesToViteConfigProperty() {
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

        val appFile = myFixture.addFileToProject(
            "assets/app.js",
            "console.log('hello');"
        )

        assertLineMarker(
            appFile,
            LineMarker.TargetAcceptsPattern(
                "Vite entry point",
                com.intellij.patterns.PlatformPatterns.psiElement(com.intellij.lang.javascript.psi.JSProperty::class.java)
            )
        )
    }

    fun testLineMarkerNavigatesToTwigFileUsingEntry() {
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

        myFixture.addFileToProject(
            "templates/base.html.twig",
            "{{ vite_entry_script_tags('app') }}"
        )

        val appFile = myFixture.addFileToProject(
            "assets/app.js",
            "console.log('hello');"
        )

        assertLineMarker(
            appFile,
            LineMarker.TargetAcceptsPattern(
                "Vite entry point",
                com.intellij.patterns.PlatformPatterns.psiFile(com.jetbrains.twig.TwigFile::class.java)
            )
        )
    }

    fun testLineMarkerNavigatesToTwigFileUsingLinkTags() {
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

        myFixture.addFileToProject(
            "templates/page.html.twig",
            "{{ vite_entry_link_tags('app') }}"
        )

        val appFile = myFixture.addFileToProject(
            "assets/app.js",
            "console.log('hello');"
        )

        assertLineMarker(
            appFile,
            LineMarker.TargetAcceptsPattern(
                "Vite entry point",
                com.intellij.patterns.PlatformPatterns.psiFile(com.jetbrains.twig.TwigFile::class.java)
            )
        )
    }
}
