package fr.adrienbrault.idea.symfony2plugin.tests.vite

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.vite.ViteJavaScriptLineMarkerProvider

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteJavaScriptLineMarkerProvider
 */
class ViteLineMarkerTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testRepriseLineMarkerNavigatesToAllFunctionsAndExistingViteUsages() {
        assertRepriseLineMarkerTargets("js", setOf(
            myFixture.addFileToProject("templates/scripts.html.twig", "{{ reprise_entry_script_tags('page/app') }}"),
            myFixture.addFileToProject("templates/links.html.twig", "{{ reprise_entry_link_tags('page/app') }}"),
            myFixture.addFileToProject("templates/js.html.twig", "{% set files = reprise_entry_js_files('page/app') %}"),
            myFixture.addFileToProject("templates/css.html.twig", "{{ reprise_entry_css_files(\"page/app\") }}"),
            myFixture.addFileToProject("templates/exists.html.twig", "{% if reprise_entry_exists('page/app') %}{% endif %}"),
            myFixture.addFileToProject("templates/vite.html.twig", "{{ vite_entry_script_tags('page/app') }}"),
            myFixture.addFileToProject("templates/mixed.html.twig", """
                {{ vite_entry_link_tags('page/app') }}
                {{ reprise_entry_link_tags('page/app') }}
                {{ reprise_entry_script_tags('page/app') }}
            """.trimIndent())
        ))
    }

    fun testRepriseLineMarkerSupportsNamedArgumentsAndTypeScriptEntries() {
        assertRepriseLineMarkerTargets("ts", setOf(
            myFixture.addFileToProject("templates/named.html.twig", "{{ reprise_entry_link_tags(entryName='page/app') }}"),
            myFixture.addFileToProject("templates/reordered.html.twig", "{{ reprise_entry_script_tags(build=null, entryName='page/app') }}"),
            myFixture.addFileToProject("templates/colon.html.twig", "{{ reprise_entry_css_files(build: null, entryName: 'page/app') }}"),
            myFixture.addFileToProject("templates/package.html.twig", "{{ reprise_entry_js_files('page/app', 'cdn', null) }}"),
            myFixture.addFileToProject("templates/attributes.html.twig", "{{ reprise_entry_link_tags('page/app', null, null, {'class': 'one,two'}) }}"),
            myFixture.addFileToProject("templates/exists.html.twig", "{% if reprise_entry_exists('page/app', null) %}{% endif %}"),
        ))
    }

    private fun assertRepriseLineMarkerTargets(extension: String, usages: Set<PsiElement>) {
        val config = myFixture.addFileToProject("vite.config.$extension", """
            export default { input: { 'page/app': './assets/app.$extension' } };
        """.trimIndent())
        val expectedTargets = mutableSetOf<PsiElement>(
            PsiTreeUtil.findChildOfType(config, JSProperty::class.java)!!.let { input ->
                PsiTreeUtil.findChildOfType(input, JSProperty::class.java)!!
            }
        )
        expectedTargets.addAll(usages)

        // These files must not appear in the popup, even though most contain the same entry literal.
        myFixture.addFileToProject("templates/excluded_other_entry.html.twig", "{{ reprise_entry_script_tags('other') }}")
        myFixture.addFileToProject("templates/excluded_named_build.html.twig", "{{ reprise_entry_script_tags('page/app', null, 'widget') }}")
        myFixture.addFileToProject("templates/excluded_exists_build.html.twig", "{{ reprise_entry_exists('page/app', 'widget') }}")
        myFixture.addFileToProject("templates/excluded_dynamic_build.html.twig", "{{ reprise_entry_link_tags(entryName='page/app', build=selectedBuild) }}")
        myFixture.addFileToProject("templates/excluded_package_only.html.twig", "{{ reprise_entry_link_tags('other', packageName='page/app') }}")
        myFixture.addFileToProject("templates/excluded_comment.html.twig", "{# reprise_entry_script_tags('page/app') #}")
        myFixture.addFileToProject("templates/excluded_unrelated.html.twig", "{{ unrelated('page/app') }}")

        val source = myFixture.addFileToProject("assets/app.$extension", "export const ready = true;")
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ViteJavaScriptLineMarkerProvider().collectSlowLineMarkers(listOf(source), markers)
        assertEquals("Vite and Reprise usages share one marker", 1, markers.size)

        val marker = markers.single()
        assertTrue(LineMarker.ToolTipEqualsAndFileAnchorAssert("Vite entry point", source).match(marker))

        val targets = (marker as RelatedItemLineMarkerInfo<*>).createGotoRelatedItems().mapNotNull { it.element }
        assertEquals("The popup contains the entry property and exactly the matching Twig files", expectedTargets, targets.toSet())
        assertEquals("Repeated usages in one template must not duplicate its target", expectedTargets.size, targets.size)
    }

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
            """

            /*
             * app bootstrap
             */
            console.log('hello');
            """.trimIndent()
        )

        assertLineMarker(appFile, LineMarker.ToolTipEqualsAndFileAnchorAssert("Vite entry point", appFile))
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
