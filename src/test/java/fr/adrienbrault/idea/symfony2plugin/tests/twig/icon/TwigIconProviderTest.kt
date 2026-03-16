package fr.adrienbrault.idea.symfony2plugin.tests.twig.icon

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.LayeredIcon
import com.intellij.util.indexing.FileBasedIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.twig.icon.TwigIconProvider
import javax.swing.Icon

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigIconProvider
 */
class TwigIconProviderTest : SymfonyLightCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("ide-twig.json")
    }

    override fun getTestDataPath(): String =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures"

    fun testIncludeTagShowsImplementsBadge() {
        myFixture.addFileToProject(
            "templates/including_test.html.twig",
            "{% include 'partial.html.twig' %}"
        )
        val partialFile = myFixture.addFileToProject(
            "templates/partial.html.twig",
            "<div>Partial content</div>"
        )

        assertIndexContains(TwigIncludeStubIndex.KEY, "partial.html.twig")

        val icon = getIconFromProvider(partialFile)
        assertNotNull("Icon should not be null for included template", icon)
        assertTrue("Icon should be a LayeredIcon for included template", icon is LayeredIcon)
    }

    fun testIncludeFunctionShowsImplementsBadge() {
        myFixture.addFileToProject(
            "templates/including_func_test.html.twig",
            "{{ include('partial_func.html.twig') }}"
        )
        val partialFile = myFixture.addFileToProject(
            "templates/partial_func.html.twig",
            "<div>Partial content</div>"
        )

        assertIndexContains(TwigIncludeStubIndex.KEY, "partial_func.html.twig")

        val icon = getIconFromProvider(partialFile)
        assertNotNull("Icon should not be null for included template", icon)
        assertTrue("Icon should be a LayeredIcon for included template", icon is LayeredIcon)
    }

    fun testSourceFunctionShowsImplementsBadge() {
        myFixture.addFileToProject(
            "templates/sourcing_test.html.twig",
            "{{ source('source_test.html.twig') }}"
        )
        val sourceFile = myFixture.addFileToProject(
            "templates/source_test.html.twig",
            "<div>Source content</div>"
        )

        assertIndexContains(TwigIncludeStubIndex.KEY, "source_test.html.twig")

        val icon = getIconFromProvider(sourceFile)
        assertNotNull("Icon should not be null for sourced template", icon)
        assertTrue("Icon should be a LayeredIcon for sourced template", icon is LayeredIcon)
    }

    fun testStandaloneTemplateDoesNotShowImplementsBadge() {
        val standaloneFile = myFixture.addFileToProject(
            "templates/_icon_provider_unique_standalone_template.html.twig",
            "<div>Standalone content</div>"
        )

        val icon = getIconFromProvider(standaloneFile)
        assertNull("Icon should be null for standalone template without special features", icon)
    }

    fun testExtendsTemplateShowsExtendsBadge() {
        myFixture.addFileToProject(
            "templates/base.html.twig",
            "<html><body>{% block content %}{% endblock %}</body></html>"
        )
        val childFile = myFixture.addFileToProject(
            "templates/child.html.twig",
            "{% extends 'base.html.twig' %}\n{% block content %}Child{% endblock %}"
        )

        assertIndexContains(TwigExtendsStubIndex.KEY, "base.html.twig")
        assertTwigFileHasExtendsIndexEntry(childFile)

        val icon = getIconFromProvider(childFile)
        assertNotNull("Icon should not be null for extending template", icon)
        assertTrue("Icon should be a LayeredIcon for extending template", icon is LayeredIcon)
    }

    fun testIncludedAndExtendingTemplateShowsBothBadges() {
        myFixture.addFileToProject(
            "templates/layout.html.twig",
            "<html><body>{% block content %}{% endblock %}</body></html>"
        )
        myFixture.addFileToProject(
            "templates/parent.html.twig",
            "{% include 'child_layout.html.twig' %}"
        )
        val childFile = myFixture.addFileToProject(
            "templates/child_layout.html.twig",
            "{% extends 'layout.html.twig' %}\n{% block content %}Content{% endblock %}"
        )

        assertIndexContains(TwigExtendsStubIndex.KEY, "layout.html.twig")
        assertIndexContains(TwigIncludeStubIndex.KEY, "child_layout.html.twig")
        assertTwigFileHasExtendsIndexEntry(childFile)

        val icon = getIconFromProvider(childFile)
        assertNotNull("Icon should not be null", icon)
        assertTrue("Icon should be a LayeredIcon", icon is LayeredIcon)
    }

    /**
     * Verify that repeated calls for the same badge combination return the exact same icon instance.
     */
    fun testSameBadgeCombinationReturnsSameIconInstance() {
        myFixture.addFileToProject(
            "templates/including_a.html.twig",
            "{% include 'shared_partial.html.twig' %}"
        )
        myFixture.addFileToProject(
            "templates/including_b.html.twig",
            "{% include 'shared_partial.html.twig' %}"
        )
        val partialFile = myFixture.addFileToProject(
            "templates/shared_partial.html.twig",
            "<div>Shared</div>"
        )

        assertIndexContains(TwigIncludeStubIndex.KEY, "shared_partial.html.twig")

        val icon1 = getIconFromProvider(partialFile)
        val icon2 = getIconFromProvider(partialFile)
        assertNotNull(icon1)
        assertSame("Same badge combination must return the identical pre-built icon instance", icon1, icon2)
    }

    private fun getIconFromProvider(psiFile: PsiFile): Icon? {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        return TwigIconProvider().getIcon(psiFile, 0)
    }

    private fun assertTwigFileHasExtendsIndexEntry(psiFile: PsiFile) {
        assertNotNull(psiFile.virtualFile)
        assertFalse(
            "Twig extends index should contain entry for file: ${psiFile.name}",
            FileBasedIndex.getInstance().getFileData(TwigExtendsStubIndex.KEY, psiFile.virtualFile!!, project).isEmpty()
        )
    }
}
