package fr.adrienbrault.idea.symfony2plugin.tests.routing.usages

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteNameReferencesSearchExecutor
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class RouteNameReferencesSearchExecutorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testFindsTwigRouteUsagesForPhpAttributeRouteDeclaration() {
        val target = myFixture.configureByText(
            "controller.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class MyController {
                #[Route('/foo', name: 'app_report_submitter<caret>')]
                public function __invoke() {}
            }
            """.trimIndent()
        ).findElementAt(myFixture.caretOffset)

        myFixture.addFileToProject("templates/path.html.twig", "{{ path('app_report_submitter') }}")
        myFixture.addFileToProject("templates/url.html.twig", "{{ url('app_report_submitter') }}")
        myFixture.addFileToProject("templates/compare.html.twig", "{% if app.request.attributes.get('_route') == 'app_report_submitter' %}{% endif %}")
        myFixture.addFileToProject("templates/same_as.html.twig", "{% if app.request.attributes.get('_route') is same as('app_report_submitter') %}{% endif %}")
        myFixture.addFileToProject("templates/in_array.html.twig", "{% if app.request.attributes.get('_route') in ['app_report_submitter', 'other_route'] %}{% endif %}")
        myFixture.addFileToProject("templates/starts_with.html.twig", "{% if app.request.attributes.get('_route') starts with 'app_report_submitter' %}{% endif %}")

        val references = getRouteUsageReferences(target!!)

        assertContainsSourceFile(references, "templates/path.html.twig")
        assertContainsSourceFile(references, "templates/url.html.twig")
        assertContainsSourceFile(references, "templates/compare.html.twig")
        assertContainsSourceFile(references, "templates/same_as.html.twig")
        assertContainsSourceFile(references, "templates/in_array.html.twig")
        assertNotContainsSourceFile(references, "templates/starts_with.html.twig")
    }

    fun testFindsPhpRouteUsagesForPhpAttributeRouteDeclaration() {
        val target = myFixture.configureByText(
            "controller.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class MyController {
                #[Route('/foo', name: 'app_report_submitter<caret>')]
                public function __invoke() {}
            }
            """.trimIndent()
        ).findElementAt(myFixture.caretOffset)

        myFixture.addFileToProject(
            "vendor/Symfony/Bundle/FrameworkBundle/Controller/AbstractController.php",
            $$"""
            <?php
            namespace Symfony\Bundle\FrameworkBundle\Controller;
            abstract class AbstractController {
                public function generateUrl(string $route, array $parameters = []) {}
                public function redirectToRoute(string $route, array $parameters = []) {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "vendor/Symfony/Component/Routing/Generator/UrlGeneratorInterface.php",
            $$"""
            <?php
            namespace Symfony\Component\Routing\Generator;
            interface UrlGeneratorInterface {
                public function generate(string $name, array $parameters = []);
            }
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "vendor/Symfony/Component/HttpKernel/Attribute/MapEntity.php",
            $$"""
            <?php
            namespace Symfony\Component\HttpKernel\Attribute;
            #[\Attribute]
            class MapEntity {
                public function __construct(?string $class = null, ?string $objectManager = null, ?string $expr = null, ?array $mapping = null, ?array $exclude = null, ?bool $stripNull = null, ?string $id = null, ?bool $evictCache = null, ?string $message = null, ?string $route = null) {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "src/Controller/PhpUsagesController.php",
            $$"""
            <?php
            namespace App\Controller;
            use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
            use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
            class PhpUsagesController extends AbstractController {
                public function index(UrlGeneratorInterface $router) {
                    $this->generateUrl('app_report_submitter');
                    $this->redirectToRoute('app_report_submitter');
                    $router->generate('app_report_submitter');
                }
            }
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "src/Controller/AttributeUsageController.php",
            $$"""
            <?php
            namespace App\Controller;
            use Symfony\Component\HttpKernel\Attribute\MapEntity;
            class AttributeUsageController {
                public function index(#[MapEntity(route: 'app_report_submitter')] object $entity) {}
            }
            """.trimIndent()
        )

        val references = getRouteUsageReferences(target!!)

        assertContainsSourceFile(references, "src/Controller/PhpUsagesController.php")
        assertContainsSourceFile(references, "src/Controller/AttributeUsageController.php")
    }

    private fun getRouteUsageReferences(target: PsiElement): Collection<RouteNameReferencesSearchExecutor.RouteUsageReference> {
        val references: Collection<PsiReference> = ReferencesSearch.search(target, GlobalSearchScope.projectScope(project)).findAll()
        return references.filterIsInstance<RouteNameReferencesSearchExecutor.RouteUsageReference>()
    }

    private fun assertContainsSourceFile(
        references: Collection<RouteNameReferencesSearchExecutor.RouteUsageReference>,
        relativePath: String,
    ) {
        for (reference in references) {
            if (reference.element.containingFile.virtualFile?.path?.endsWith(relativePath) == true) {
                return
            }
        }

        fail("Expected reference from file: $relativePath")
    }

    private fun assertNotContainsSourceFile(
        references: Collection<RouteNameReferencesSearchExecutor.RouteUsageReference>,
        relativePath: String,
    ) {
        for (reference in references) {
            if (reference.element.containingFile.virtualFile?.path?.endsWith(relativePath) == true) {
                fail("Did not expect reference from file: $relativePath")
            }
        }
    }
}
