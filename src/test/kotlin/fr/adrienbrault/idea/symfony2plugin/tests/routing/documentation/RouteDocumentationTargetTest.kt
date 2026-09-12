package fr.adrienbrault.idea.symfony2plugin.tests.routing.documentation

import com.intellij.model.Pointer
import com.intellij.find.actions.FindUsagesAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PlatformTestUtil
import fr.adrienbrault.idea.symfony2plugin.documentation.DocumentationFindUsagesLinkHandler
import fr.adrienbrault.idea.symfony2plugin.routing.documentation.*
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class RouteDocumentationTargetTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject("config/routes.yaml", """
            app_blog:
              path: /blog/{slug}/{page}
              methods: [GET]
              defaults: { page: 0 }
              requirements: { slug: '[a-z<>]+' }
        """.trimIndent())
    }

    private fun doc(text: String, file: String = "test.html.twig"): String? {
        val psi = myFixture.configureByText(file, text)
        val targets = RouteDocumentationTargetProvider().documentationTargets(psi, myFixture.caretOffset)
        val target = targets.singleOrNull() ?: return null

        return computeDocumentationBlocking(Pointer.hardPointer(target))?.html
    }

    fun testPointerRestoresDocumentation() {
        val psi = myFixture.configureByText("test.html.twig", "{{ path('app_<caret>blog') }}")
        val target = RouteDocumentationTargetProvider().documentationTargets(psi, myFixture.caretOffset).single() as RouteDocumentationTarget

        val restored = target.createPointer().dereference()
        assertNotNull(restored)
        assertTrue(computeDocumentationBlocking(Pointer.hardPointer(restored!!))!!.html.contains("/blog/{slug}/{page}"))
    }

    fun testXmlRoute() {
        myFixture.addFileToProject("config/routes.xml", """
            <routes><route id="xml_route" path="/{page}" methods="POST">
                <default key="page">0</default>
                <requirement key="page">[0-9]+</requirement>
            </route></routes>
        """.trimIndent())

        val html = doc("{{ url('xml_<caret>route') }}")!!
        assertTrue(html.contains("Methods: <code>POST</code>"))
        assertFalse(html.contains("<code>0</code>"))
    }

    fun testAttributeRouteWithClassPrefix() {
        myFixture.addFileToProject("src/Blog.php", """
            <?php
            namespace App;
            use Symfony\Component\Routing\Attribute\Route;
            #[Route('/prefix', name: 'prefix_', requirements: ['page' => '[0-9]+'])]
            class Blog {
                #[Route('/{page}/{empty}/{nothing}', name: 'attribute', defaults: ['page' => 0, 'empty' => '', 'nothing' => null])]
                public function show() {}
            }
        """.trimIndent())

        val html = doc("{{ path('prefix_attr<caret>ibute') }}")!!
        assertTrue(html.contains("/prefix/{page}/{empty}/{nothing}"))
        assertFalse(html.contains("<code>0</code>"))
        assertTrue(html.contains("App\\Blog::show"))
    }

    fun testAnnotationRoute() {
        myFixture.addFileToProject("src/Annotated.php", """
            <?php
            namespace App;
            use Symfony\Component\Routing\Annotation\Route;
            class Annotated {
                /** @Route("/{page}", name="annotated", defaults={"page"=0}, requirements={"page"="[0-9]+"}) */
                public function show() {}
            }
        """.trimIndent())

        val html = doc("{{ path('annot<caret>ated') }}")!!
        assertTrue(html, html.contains("/{page}"))
        assertFalse(html.contains("<code>0</code>"))
    }

    fun testFluentPhpRouteWithLocalAssignment() {
        myFixture.addFileToProject("config/routes.php", $$"""
            <?php
            use Symfony\Component\Routing\Loader\Configurator\RoutingConfigurator;
            return static function (RoutingConfigurator $routes): void {
                $route = $routes->prefix('/prefix')->add('fluent', '/{page}');
                $route->defaults(['page' => 0])->requirements(['page' => '[0-9]+']);
            };
        """.trimIndent())

        val html = doc("{{ path('flu<caret>ent') }}")!!
        assertTrue(html, html.contains("/{page}"))
        assertFalse(html.contains("<code>0</code>"))
    }

    fun testInlineAndDuplicateRoutes() {
        myFixture.addFileToProject("config/inline.yaml", """
            inline:
              path: '/{!page<[0-9]{1,3}>?0}/{slug?hello}'
            app_blog:
              path: /other/{slug}
              requirements: { slug: '[A-Z]+' }
        """.trimIndent())

        val inline = doc("{{ path('in<caret>line') }}")!!
        assertTrue(inline, inline.contains("/{!page&lt;[0-9]{1,3}&gt;?0}/{slug?hello}"))

        val duplicate = doc("{{ path('app_<caret>blog') }}")!!
        assertTrue(duplicate, duplicate.contains("/other/{slug}"))
        assertTrue(duplicate, duplicate.contains("/blog/{slug}/{page}"))
    }

    fun testTwigUsageFiles() {
        myFixture.addFileToProject("templates/blog.html.twig", "{{ path('app_blog') }} {{ url('app_blog') }}")
        myFixture.addFileToProject("templates/menu.html.twig", "{{ path('app_blog') }}")
        myFixture.addFileToProject("templates/other.html.twig", "{{ path('other') }}")

        val html = doc("{{ path('app_<caret>blog') }}")!!
        // The current template also uses the route; repeated calls count as one file.
        assertTrue(html, html.contains("<br>Twig usages: 3"))
    }

    fun testMissingRoute() {
        val psi = myFixture.configureByText("test.html.twig", "{{ path('missing<caret>') }}")
        val context = RouteDocumentationTargetProvider().resolveContext(psi, myFixture.caretOffset)!!
        val target = RouteDocumentationTarget(context)

        assertNull(computeDocumentationBlocking(Pointer.hardPointer(target)))
    }

    fun testFindUsagesLinkUsesExistingRouteTargets() {
        val file = myFixture.configureByText("test.html.twig", "{{ path('app_<caret>blog') }}")
        val target = RouteDocumentationTargetProvider().documentationTargets(file, myFixture.caretOffset).single() as RouteDocumentationTarget
        val html = computeDocumentationBlocking(Pointer.hardPointer(target))!!.html

        assertTrue(html.contains("href=\"$ROUTE_FIND_USAGES_LINK\">Find usages</a>"))
        assertTrue(html.indexOf("Find usages</a>") > html.indexOf("Twig usages:"))

        val context = target.findUsagesContext()!!
        val actual = context.getData(com.intellij.usages.UsageView.USAGE_TARGETS_KEY)!!
            .map { (it as com.intellij.usages.PsiElementUsageTarget).element }

        val expected = fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteUsageTargetProvider()
            .getTargets(file.findElementAt(myFixture.caretOffset)!!)
            .map { (it as com.intellij.usages.PsiElementUsageTarget).element }

        assertEquals(expected, actual)
        assertTrue(actual.all { fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteFindUsagesHandlerFactory().canFindUsages(it!!) })
        assertNull(DocumentationFindUsagesLinkHandler().resolveLink(target, "unrelated-link"))
        assertNull(DocumentationFindUsagesLinkHandler().resolveLink(target, fr.adrienbrault.idea.symfony2plugin.templating.documentation.TEMPLATE_FIND_USAGES_LINK))
    }

    fun testFindUsagesContextKeepsAllRouteDeclarations() {
        myFixture.addFileToProject("config/other.yaml", "app_blog: { path: /other }")
        val file = myFixture.configureByText("test.html.twig", "{{ path('app_<caret>blog') }}")
        val target = RouteDocumentationTargetProvider().documentationTargets(file, myFixture.caretOffset).single() as RouteDocumentationTarget

        val context = target.findUsagesContext()!!
        assertEquals(2, context.getData(com.intellij.usages.UsageView.USAGE_TARGETS_KEY)!!.size)
    }

    fun testFindUsagesLinkStartsRouteSearch() {
        val file = myFixture.configureByText("test.html.twig", "{{ path('app_<caret>blog') }}")
        val target = RouteDocumentationTargetProvider().documentationTargets(file, myFixture.caretOffset).single()
        val manager = ActionManager.getInstance()
        val original = manager.getAction(IdeActions.ACTION_FIND_USAGES)
        var searched: PsiElement? = null

        manager.replaceAction(IdeActions.ACTION_FIND_USAGES, object : FindUsagesAction() {
            override fun startFindUsages(element: PsiElement) {
                searched = element
            }
        })
        
        try {
            assertNotNull(DocumentationFindUsagesLinkHandler().resolveLink(target, ROUTE_FIND_USAGES_LINK))
            PlatformTestUtil.waitWithEventsDispatching("Route search did not start", { searched != null }, 10)
            assertTrue(fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteFindUsagesHandlerFactory().canFindUsages(searched!!))
        } finally {
            manager.replaceAction(IdeActions.ACTION_FIND_USAGES, original)
        }
    }

    fun testFindUsagesContextRevalidatesDeletedRoute() {
        val file = myFixture.configureByText("test.html.twig", "{{ path('app_<caret>blog') }}")
        val target = RouteDocumentationTargetProvider().documentationTargets(file, myFixture.caretOffset).single() as RouteDocumentationTarget
        assertNotNull(target.findUsagesContext())

        val routeFile = myFixture.findFileInTempDir("config/routes.yaml")
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) { routeFile.delete(this) }
        assertNull(target.findUsagesContext())
    }

}
