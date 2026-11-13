package fr.adrienbrault.idea.symfony2plugin.tests.translation.usages

import com.intellij.codeInsight.navigation.actions.GotoDeclarationOrUsageHandler2
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.usages.TranslationKeyFindUsagesHandlerFactory
import fr.adrienbrault.idea.symfony2plugin.translation.usages.TranslationKeyUsageUtil

class TranslationKeyFindUsagesIntegrationTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testCtrlClickOnYamlKeyShowsUsages() {
        configureByProjectPath("translations/messages.en.yaml", "home.oss.li<caret>ne.on: 'on'")

        assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())
    }

    fun testCtrlClickOnPhpTranslationKeyShowsUsages() {
        configureByProjectPath("translations/messages.en.php", "<?php return ['home.oss.li<caret>ne.on' => 'on'];")

        assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())
    }

    fun testCtrlClickOnXliffSourceAndResnameShowsUsages() {
        withXliffAsXml {
            val content = """
                <?xml version="1.0"?>
                <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
                    <file source-language="en" datatype="plaintext" original="file.ext">
                        <body>
                            <trans-unit id="a1" resname="RESNAME"><source>SOURCE</source><target>on</target></trans-unit>
                        </body>
                    </file>
                </xliff>
            """.trimIndent()

            configureByProjectPath("translations/messages.en.xlf", content.replace("SOURCE", "home.oss.li<caret>ne.on").replace("RESNAME", "home.oss.line.on"))
            assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())

            configureByProjectPath("translations/validators.en.xlf", content.replace("RESNAME", "name.not_bl<caret>ank").replace("SOURCE", "name.not_blank"))
            assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())
        }
    }

    fun testXliffSourceUsagesAreFound() {
        withXliffAsXml {
            configureByProjectPath(
                "translations/messages.en.xlf",
                """
                <?xml version="1.0"?>
                <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
                    <file source-language="en" datatype="plaintext" original="file.ext">
                        <body>
                            <trans-unit id="a1"><source>home.oss.li<caret>ne.on</source><target>on</target></trans-unit>
                        </body>
                    </file>
                </xliff>
                """.trimIndent()
            )
            myFixture.addFileToProject("templates/filter.html.twig", "{{ 'home.oss.line.on'|trans }}")

            assertContainsUsageFile(findUsages(), "templates/filter.html.twig")
        }
    }

    fun testFindUsagesTargetPresentation() {
        configureByProjectPath("translations/messages+intl-icu.en.yaml", "home.oss.li<caret>ne.on: 'on'")

        val handler = createHandler()
        val primaryTarget = handler.primaryElements[0] as NavigationItem

        assertEquals("home.oss.line.on", primaryTarget.presentation!!.presentableText)
        assertEquals("messages, translations/messages+intl-icu.en.yaml", primaryTarget.presentation!!.locationString)
    }

    fun testNonTranslationYamlKeyIsIgnored() {
        configureByProjectPath("config/packages/framework.yaml", "framework:\n    sec<caret>ret: foo")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        assertNull(TranslationKeyUsageUtil.getDeclaration(element))
        assertFalse(TranslationKeyFindUsagesHandlerFactory().canFindUsages(element.parent))
    }

    private fun gtduOutcome(): GotoDeclarationOrUsageHandler2.GTDUOutcome? =
        GotoDeclarationOrUsageHandler2.testGTDUOutcome(myFixture.editor, myFixture.file, myFixture.caretOffset)

    private fun createHandler() = TranslationKeyFindUsagesHandlerFactory().let { factory ->
        val declaration = TranslationKeyUsageUtil.getDeclaration(myFixture.file.findElementAt(myFixture.caretOffset)!!)
        assertNotNull(declaration)
        assertTrue(factory.canFindUsages(declaration!!.element))
        factory.createFindUsagesHandler(declaration.element, false)!!
    }

    private fun findUsages(): List<UsageInfo> {
        val handler = createHandler()
        val options = handler.findUsagesOptions
        options.searchScope = GlobalSearchScope.projectScope(project)
        options.isSearchForTextOccurrences = false
        options.isUsages = true

        val usages = mutableListOf<UsageInfo>()
        assertTrue(handler.processElementUsages(handler.primaryElements[0], { usages.add(it) }, options))
        return usages
    }

    private fun configureByProjectPath(filePath: String, content: String) {
        val caretOffset = content.indexOf("<caret>")
        assertTrue("Missing <caret> marker", caretOffset >= 0)

        myFixture.addFileToProject(filePath, content.replace("<caret>", ""))
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(filePath))
        myFixture.editor.caretModel.moveToOffset(caretOffset)
    }

    private fun withXliffAsXml(block: () -> Unit) {
        WriteAction.runAndWait<RuntimeException> { FileTypeManagerEx.getInstanceEx().associateExtension(XmlFileType.INSTANCE, "xlf") }
        try {
            block()
        } finally {
            WriteAction.runAndWait<RuntimeException> { FileTypeManager.getInstance().removeAssociatedExtension(XmlFileType.INSTANCE, "xlf") }
        }
    }

    private fun assertContainsUsageFile(usages: List<UsageInfo>, relativePath: String) {
        val paths = usages.map { it.virtualFile?.path ?: "<no-path>" }
        assertTrue("Expected usage in $relativePath; actual: $paths", paths.any { it.endsWith("/$relativePath") })
    }
}
