package fr.adrienbrault.idea.symfony2plugin.tests.templating.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.psi.PsiFile
import com.intellij.testFramework.DumbModeTestUtils
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.templating.documentation.*
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigTemplateDocumentationTargetProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    private lateinit var template: PsiFile

    override fun setUp() {
        super.setUp()

        template = myFixture.addFileToProject("templates/blog.html.twig", "{% extends 'layout.html.twig' %}")
        myFixture.addFileToProject("templates/layout.html.twig", "")

        myFixture.addFileToProject("ide-twig.json", """
            {"namespaces": [
                {"path": "templates"},
                {"path": "templates", "namespace": "Blog"},
                {"path": "templates", "type": "bundle"},
                {"path": "templates", "namespace": "BlogBundle", "type": "bundle"}
            ]}
        """.trimIndent())

        myFixture.addFileToProject("controller.php", """
            <?php
            namespace Symfony\Bundle\FrameworkBundle\Controller;
            class AbstractController {
                public function render(${'$'}view, array ${'$'}parameters = []) {}
                public function renderView(${'$'}view, array ${'$'}parameters = []) {}
            }
        """.trimIndent())
    }

    private fun target(text: String, filename: String = "test.html.twig"): TwigTemplateDocumentationTarget? {
        val file = myFixture.configureByText(filename, text)

        return TwigTemplateDocumentationTargetProvider().documentationTargets(file, myFixture.caretOffset)
            .singleOrNull() as? TwigTemplateDocumentationTarget
    }

    private fun html(target: TwigTemplateDocumentationTarget): String =
        computeDocumentationBlocking(Pointer.hardPointer(target))!!.html

    fun testTwigContextsAndQuoteBoundaries() {
        val usages = listOf(
            "{% extends ARG %}", "{% include ARG %}", "{{ include(ARG) }}", "{% embed ARG %}{% endembed %}",
            "{% import ARG as macros %}", "{% from ARG import field %}", "{{ source(ARG) }}",
            "{% form_theme form ARG %}", "{% form_theme form with [ARG] %}", "{{ block('content', ARG) }}",
            "{% include ['missing.html.twig', ARG] %}", "{% include condition ? ARG : 'missing.html.twig' %}",
            "{% extends condition ? 'missing.html.twig' : ARG %}",
        )
        for (usage in usages) for (argument in listOf("'blog<caret>.html.twig'", "<caret>'blog.html.twig'", "'blog.html.twig<caret>'")) {
            val text = usage.replace("ARG", argument)
            assertNotNull(text, target(text))
        }
    }

    fun testNamespaceAndLegacyNames() {
        for (name in listOf("blog.html.twig", "::blog.html.twig", "@Blog/blog.html.twig", "@!Blog/blog.html.twig", "BlogBundle::blog.html.twig")) {
            val candidate = target("{% include '$name<caret>' %}")
            assertNotNull(name, candidate)
            val result = candidate!!
            assertEquals(name, result.computePresentation().presentableText)
            assertEquals(template, result.navigatable)
        }
    }

    fun testPhpArgumentsAndTemplateMetadata() {
        myFixture.addFileToProject("template_attributes.php", """
            <?php
            namespace Symfony\Bridge\Twig\Attribute { class Template { function __construct(${'$'}template = null) {} } }
            namespace Sensio\Bundle\FrameworkExtraBundle\Configuration { /** @Annotation */ class Template {} }
        """.trimIndent())
        val prefix = "<?php class Blog extends \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController { function index() { "
        for (call in listOf(
            "\$this->render('blog<caret>.html.twig');",
            "\$this->renderView('blog<caret>.html.twig');",
            "\$this->render(parameters: [], view: 'blog<caret>.html.twig');",
        )) assertNotNull(call, target(prefix + call + " }}", "test.php"))
        for (metadata in listOf(
            "#[\\Symfony\\Bridge\\Twig\\Attribute\\Template('blog<caret>.html.twig')]",
            "/** @\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template(\"blog<caret>.html.twig\") */",
        )) assertNotNull(metadata, target("<?php class Blog { $metadata public function index() {} }", "test.php"))
    }

    fun testNegativeContexts() {
        for (text in listOf(
            "{{ 'blog<caret>.html.twig' }}", "{{ asset('blog<caret>.html.twig') }}",
            "{% include 'missing<caret>.html.twig' %}", "{% include 'blog<caret>.html.twig' ~ suffix %}",
            "{% include 'blog<caret>#{suffix}.html.twig' %}",
            "{% include 'blog.html.twig' with {'value': 'blog<caret>.html.twig'} %}",
            "{{ block('blog<caret>.html.twig') }}", "{% include 'blog.html.twig' <caret>with {} %}",
            "{% use 'blog<caret>.html.twig' %}",
        )) assertNull(text, target(text))
        for (text in listOf(
            "<?php \$x = 'blog<caret>.html.twig';",
            "<?php (new \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController)->render('blog<caret>.html.twig' . \$suffix);",
            "<?php (new \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController)->render(\"blog<caret>\$suffix.html.twig\");",
        )) assertNull(text, target(text, "test.php"))
    }

    fun testDisabledPlugin() {
        val result = target("{% include 'blog<caret>.html.twig' %}")!!
        Settings.getInstance(project).pluginEnabled = false
        try {
            assertNull(result.createPointer().dereference())
            assertNull(result.navigatable)
            assertNull(computeDocumentationBlocking(Pointer.hardPointer(result)))
        } finally {
            Settings.getInstance(project).pluginEnabled = true
        }
    }

    fun testLoadAndEmailContextsDoNotClaimCompletePhpCoverage() {
        myFixture.addFileToProject("environment.php", """
            <?php
            namespace Twig { class Environment { function load(${'$'}name) {} } }
            namespace Symfony\Bridge\Twig\Mime { class TemplatedEmail { function htmlTemplate(${'$'}template) {} } }
        """.trimIndent())
        for (call in listOf(
            "(new \\Twig\\Environment)->load(name: 'blog<caret>.html.twig')",
            "(new \\Symfony\\Bridge\\Twig\\Mime\\TemplatedEmail)->htmlTemplate(template: 'blog<caret>.html.twig')",
        )) {
            val result = target("<?php $call;", "test.php")
            assertNotNull(call, result)
            val rendered = html(result!!)
            assertTrue(rendered, rendered.contains("PHP:<br>Twig:"))
            assertTrue(rendered, rendered.contains("Twig:</div>"))
        }
    }

    fun testDumbMode() {
        val file = myFixture.configureByText("test.html.twig", "{% include 'blog<caret>.html.twig' %}")
        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            assertEmpty(TwigTemplateDocumentationTargetProvider().documentationTargets(file, myFixture.caretOffset))
        }
    }

    fun testRendererLimitsEscapingAndZeroUsages() {
        for (count in listOf(0, 1, 5, 6)) {
            val callers = (1..count).reversed().map { "Caller$it::<render>" }.toSet()
            val data = TwigTemplateDocumentationData(
                "<template>", mapOf("file:///path?a=1&b=2" to "<path>"),
                phpFiles = (1..count).map { "file$it.php" }.toSet(), callers = callers,
            )
            val rendered = renderTwigTemplateDocumentation(data)
            assertFalse(rendered.contains("&lt;template&gt;"))
            assertTrue(rendered.contains("&lt;path&gt;"))
            assertTrue(rendered.contains("PHP:<br>"))
            assertTrue(rendered.contains("Twig:"))
            assertEquals(count > 0, rendered.contains("Find usages</a>"))
            assertEquals(count > 5, rendered.contains("5 / $count"))
            assertEquals(count > 5, rendered.contains("..."))
            assertFalse(rendered.contains("Caller6"))
            if (count > 0) assertTrue(rendered.contains("- <code>Caller1::&lt;render&gt;</code>"))
            if (count >= 5) assertTrue(rendered.indexOf("Caller1") < rendered.indexOf("Caller5"))

            if (count > 1) {
                val fewerFiles = renderTwigTemplateDocumentation(data.copy(phpFiles = setOf("shared.php")))
                assertTrue(fewerFiles.contains("PHP: 1 files"))
            }
        }
    }
}
