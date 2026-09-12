package fr.adrienbrault.idea.symfony2plugin.tests.templating.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.psi.PsiFile
import fr.adrienbrault.idea.symfony2plugin.templating.documentation.*
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigTemplateDocumentationTargetTest : SymfonyLightCodeInsightFixtureTestCase() {
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

    private fun data(text: String = "{% include 'blog<caret>.html.twig' %}"): TwigTemplateDocumentationData {
        val file = myFixture.configureByText("test.html.twig", text)
        val context = TwigTemplateDocumentationTargetProvider().resolveContext(file, myFixture.caretOffset)!!

        return collectTwigTemplateDocumentation(context)
    }

    fun testDirectIndexedUsagesAndDistinctFileCounts() {
        myFixture.addFileToProject("templates/caller.html.twig", """
            {% include 'blog.html.twig' %}
            {% embed 'blog.html.twig' %}{% endembed %}
            {% extends 'blog.html.twig' %}
        """.trimIndent())
        myFixture.addFileToProject("templates/second.html.twig", "{% include '@!Blog/blog.html.twig' %}")

        val result = data()
        assertEquals(setOf("include", "embed", "extends"), result.twigUsages.keys)
        assertEquals(3, result.twigUsages.values.flatten().toSet().size)
        // The existing index retains only the last include type for each name/file.
        assertEquals(2, result.twigUsages.getValue("include").size)
        assertEquals(1, result.twigUsages.getValue("embed").size)
        assertEquals(setOf("layout.html.twig"), result.parents.getValue(template.virtualFile.url))

        val rendered = renderTwigTemplateDocumentation(result)
        assertTrue(rendered.contains("Roles: extends, included, extended"))
        assertTrue(rendered.contains("- Extended by: 1 files"))
    }

    fun testOnlyDirectUsagesAreCounted() {
        val direct = myFixture.addFileToProject("templates/direct.html.twig", "{% extends 'blog.html.twig' %}")
        myFixture.addFileToProject("templates/indirect.html.twig", "{% extends 'direct.html.twig' %}")
        myFixture.addFileToProject("templates/indirect_include.html.twig", "{% include 'direct.html.twig' %}")
        myFixture.addFileToProject("templates/layout_caller.html.twig", "{% include 'layout.html.twig' %}")

        val result = data()
        assertEquals(setOf(direct.virtualFile.url), result.twigUsages.getValue("extends"))
        assertEquals(1, result.twigUsages.getValue("include").size)
        assertEquals(2, result.twigUsages.values.flatten().toSet().size)
    }

    fun testGroupedIndexTypesAreDisplayedHonestly() {
        myFixture.addFileToProject("templates/source.html.twig", "{{ source('blog.html.twig') }}")
        myFixture.addFileToProject("templates/from.html.twig", "{% from 'blog.html.twig' import field %}")

        val result = data()
        assertEquals(1, result.twigUsages.getValue("include() / source()").size)
        assertEquals(1, result.twigUsages.getValue("import / from").size)
    }

    fun testPhpScopesAndFileCountsAreIndependent() {
        myFixture.addFileToProject("src/Preview.php", """
            <?php
            class Preview extends \Symfony\Bundle\FrameworkBundle\Controller\AbstractController {
                function first() { ${'$'}this->render('blog.html.twig'); ${'$'}this->render('blog.html.twig'); }
                function second() { ${'$'}this->render('@Blog/blog.html.twig'); }
            }
        """.trimIndent())

        val result = data()
        assertEquals(1, result.phpFiles.size)
        assertEquals(setOf("Preview::first", "Preview::second"), result.callers)
    }

    fun testNavigationPointerAndDeletion() {
        val target = target("{% include 'blog<caret>.html.twig' %}")!!
        val pointer = target.createPointer()

        assertEquals(template, target.navigatable)
        assertEquals(template, pointer.dereference()!!.navigatable)
        assertTrue(target.navigatable!!.canNavigate())
        target.navigatable!!.navigate(true)
        assertContainsElements(com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedFiles.toList(), template.virtualFile)

        assertTrue(html(target).contains("layout.html.twig"))
        WriteCommandAction.runWriteCommandAction(project) { template.delete() }
        assertNull(pointer.dereference())
        assertNull(target.navigatable)
        assertNull(computeDocumentationBlocking(Pointer.hardPointer(target)))
    }

    fun testMultipleTargetsUsePlatformSelection() {
        val other = myFixture.addFileToProject("overrides/blog.html.twig", "")
        myFixture.addFileToProject("overrides/ide-twig.json", """{"namespaces": [{"path": "."}]}""")
        val file = myFixture.configureByText("test.html.twig", "{% include 'blog<caret>.html.twig' %}")
        val targets = TwigTemplateDocumentationTargetProvider().documentationTargets(file, myFixture.caretOffset)
        assertEquals(2, targets.size)
        assertEquals(setOf(template, other), targets.map { it.navigatable }.toSet())
        assertEquals(2, targets.map { it.computePresentation().locationText }.toSet().size)

        for (result in targets) {
            val rendered = html(result as TwigTemplateDocumentationTarget)
            assertTrue(rendered.contains("templates/blog.html.twig"))
            assertTrue(rendered.contains("overrides/blog.html.twig"))
            assertTrue(rendered.contains("ambiguous"))
            assertEquals(result.navigatable, result.createPointer().dereference()!!.navigatable)
        }
    }

    fun testUsageDeletionUpdatesDocumentation() {
        val caller = myFixture.addFileToProject("templates/caller.html.twig", "{% embed 'blog.html.twig' %}{% endembed %}")
        val file = myFixture.configureByText("test.html.twig", "{% include 'blog<caret>.html.twig' %}")
        val context = TwigTemplateDocumentationTargetProvider().resolveContext(file, myFixture.caretOffset)!!
        assertEquals(1, collectTwigTemplateDocumentation(context).twigUsages.getValue("embed").size)
        WriteCommandAction.runWriteCommandAction(project) { caller.delete() }
        assertFalse(collectTwigTemplateDocumentation(context).twigUsages.containsKey("embed"))
    }

    fun testParentEditInvalidatesDocumentation() {
        val result = target("{% include 'blog<caret>.html.twig' %}")!!
        assertTrue(html(result).contains("Extends:"))
        val document = myFixture.getDocument(template)
        WriteCommandAction.runWriteCommandAction(project) {
            document.setText("plain template")
            com.intellij.psi.PsiDocumentManager.getInstance(project).commitAllDocuments()
        }
        assertFalse(html(result).contains("Extends:"))
    }

    fun testFindUsagesLinkTargetsTemplateFile() {
        val caller = myFixture.addFileToProject("templates/caller.html.twig", "{% include 'blog.html.twig' %}")
        val result = target("{% include 'blog<caret>.html.twig' %}")!!
        val rendered = html(result)
        assertTrue(rendered.contains("href=\"$TEMPLATE_FIND_USAGES_LINK\">Find usages</a>"))
        assertFalse(rendered.contains("<br><br>"))
        assertTrue(rendered.indexOf("Find usages</a>") > rendered.indexOf("- include:"))

        val context = result.findUsagesContext()!!
        val usageTarget = context.getData(com.intellij.usages.UsageView.USAGE_TARGETS_KEY)!!.single()
            as com.intellij.usages.PsiElementUsageTarget
        assertEquals(template, usageTarget.element)

        val usages = com.intellij.psi.search.searches.ReferencesSearch.search(usageTarget.element!!).findAll()
        assertTrue(usages.any { it.element.containingFile == caller })

        WriteCommandAction.runWriteCommandAction(project) { template.delete() }
        assertNull(result.findUsagesContext())
    }

}
