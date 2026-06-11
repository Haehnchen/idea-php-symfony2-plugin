package fr.adrienbrault.idea.symfony2plugin.tests.ux

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiFile
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentDefinition
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentProvider
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentProviderParameter
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigComponentCollector
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase.LineMarker
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil

/**
 * @see TwigComponentProvider
 */
class TwigComponentProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testExternalAnonymousComponentIsIncludedInCompletionAndCollector() {
        val templateFile = addTemplate(
            "external/package/templates/components/Button/Primary.html.twig",
            "{% props label, size = 'md' %}\n{% block content %}{% endblock %}",
        )
        registerProvider(TwigComponentDefinition("ExternalPackage:Button:Primary", templateFile.virtualFile))

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ component('<caret>') }}",
            "ExternalPackage:Button:Primary",
        )

        val result = TwigComponentCollector(project).collect("ExternalPackage")
        assertTrue(result.contains("<twig:ExternalPackage:Button:Primary></twig:ExternalPackage:Button:Primary>"))
        assertTrue(result.contains("{{ component('ExternalPackage:Button:Primary') }}"))
        assertTrue(result.contains("external/package/templates/components/Button/Primary.html.twig"))
        assertTrue(result.contains("label;size"))
        assertTrue(result.contains("content"))
    }

    fun testExternalIndexTemplateComponentResolvesToProviderFile() {
        val templateFile = addTemplate(
            "external/package/templates/components/Button/index.html.twig",
            "<button></button>",
        )
        registerProvider(TwigComponentDefinition("ExternalPackage:Button", templateFile.virtualFile))

        val templates = UxUtil.getComponentTemplates(project, "ExternalPackage:Button")

        assertTrue(templates.any { it.virtualFile == templateFile.virtualFile })
    }

    fun testExternalComponentNavigationUsesProviderTemplate() {
        addProviderTemplate()

        assertNavigationContainsFile(
            TwigFileType.INSTANCE,
            "{{ component('ExternalPackage:Button:Prim<caret>ary') }}",
            "external/package/templates/components/Button/Primary.html.twig",
        )
    }

    fun testProviderPhpClassAddsTemplateVariables() {
        val templateFile = addTemplate(
            "external/package/templates/components/Button/Primary.html.twig",
            "{{ <caret> }}",
        )
        addProviderPhpClass()
        registerProvider(
            TwigComponentDefinition(
                name = "ExternalPackage:Button:Primary",
                template = templateFile.virtualFile,
                phpClassFqn = "\\ExternalPackage\\Components\\Button\\Primary",
            ),
        )

        myFixture.configureFromTempProjectFile("external/package/templates/components/Button/Primary.html.twig")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings ?: emptyList(), "attributes", "this", "label", "size")
        assertDoesntContain(myFixture.lookupElementStrings ?: emptyList(), "secret")
    }

    fun testProviderPhpClassLineMarkerUsesProviderTemplate() {
        val templateFile = addProviderTemplate(register = false)
        val phpFile = addProviderPhpClass()
        registerProvider(
            TwigComponentDefinition(
                name = "ExternalPackage:Button:Primary",
                template = templateFile.virtualFile,
                phpClassFqn = "\\ExternalPackage\\Components\\Button\\Primary",
            ),
        )

        assertLineMarker(phpFile, LineMarker.ToolTipEqualsAssert("Navigate to UX Component template"))
        assertLineMarker(
            phpFile,
            LineMarker.TargetAcceptsPattern(
                "Navigate to UX Component template",
                PlatformPatterns.psiFile().withName("Primary.html.twig"),
            ),
        )
    }

    private fun addProviderTemplate(register: Boolean = true): PsiFile {
        val templateFile = addTemplate(
            "external/package/templates/components/Button/Primary.html.twig",
            "{% props label, size = 'md' %}\n{% block content %}{% endblock %}",
        )

        if (register) {
            registerProvider(TwigComponentDefinition("ExternalPackage:Button:Primary", templateFile.virtualFile))
        }

        return templateFile
    }

    private fun addTemplate(path: String, content: String): PsiFile =
        myFixture.addFileToProject(path, content)

    private fun addProviderPhpClass(): PsiFile =
        myFixture.addFileToProject(
            "src/ExternalPackage/Components/Button/Primary.php",
            """
            <?php
            namespace ExternalPackage\Components\Button;

            class Primary
            {
                public string ${'$'}label = '';
                public string ${'$'}size = '';
                private string ${'$'}secret = '';
            }
            """.trimIndent(),
        )

    private fun registerProvider(vararg definitions: TwigComponentDefinition) {
        UxUtil.TWIG_COMPONENT_PROVIDERS.point.registerExtension(
            TestProvider(definitions.toList()),
            testRootDisposable,
        )
    }

    private class TestProvider(
        private val definitions: Collection<TwigComponentDefinition>,
    ) : TwigComponentProvider {
        override fun getComponents(parameter: TwigComponentProviderParameter): Collection<TwigComponentDefinition> = definitions
    }
}
