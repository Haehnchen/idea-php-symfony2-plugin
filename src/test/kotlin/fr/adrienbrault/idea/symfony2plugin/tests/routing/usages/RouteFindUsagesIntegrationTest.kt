package fr.adrienbrault.idea.symfony2plugin.tests.routing.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlAttribute
import com.intellij.usageView.UsageInfo
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTarget
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteFindUsagesHandlerFactory
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteUsageTargetProvider
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import org.jetbrains.yaml.psi.YAMLKeyValue

class RouteFindUsagesIntegrationTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testTwigPathFindUsagesTargetsPhpAttributeDeclaration() {
        myFixture.addFileToProject(
            "src/Controller/HomeController.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class HomeController {
                #[Route('/home', name: 'home')]
                public function index() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{{ url('home') }}")

        val result = findUsagesFromTwigRouteUsage("templates/index.html.twig", "{{ path('ho<caret>me') }}")

        assertInstanceOf(result.targetElement, com.jetbrains.php.lang.psi.elements.StringLiteralExpression::class.java)
        assertRouteTargetPresentation(result.primaryTarget, "home", "/home, src/Controller/HomeController.php")
        assertContainsUsageFile(result.usages, "templates/index.html.twig")
        assertContainsUsageFile(result.usages, "templates/secondary.html.twig")
    }

    fun testTwigPathFindUsagesTargetsClassLevelPhpAttributeDeclaration() {
        myFixture.addFileToProject(
            "src/Controller/HomeController.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            #[Route('/home', name: 'home')]
            class HomeController {
                public function __invoke() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{{ url('home') }}")

        val result = findUsagesFromTwigRouteUsage("templates/index.html.twig", "{{ path('ho<caret>me') }}")

        assertInstanceOf(result.targetElement, com.jetbrains.php.lang.psi.elements.StringLiteralExpression::class.java)
        assertRouteTargetPresentation(result.primaryTarget, "home", "/home, src/Controller/HomeController.php")
        assertContainsUsageFile(result.usages, "templates/index.html.twig")
        assertContainsUsageFile(result.usages, "templates/secondary.html.twig")
    }

    fun testTwigPathFindUsagesTargetsYamlDeclaration() {
        myFixture.addFileToProject(
            "config/routes.yaml",
            """
            home:
                path: /home
                controller: App\Controller\HomeController::index
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{{ url('home') }}")

        val result = findUsagesFromTwigRouteUsage("templates/index.html.twig", "{{ path('ho<caret>me') }}")

        assertInstanceOf(result.targetElement, YAMLKeyValue::class.java)
        assertRouteTargetPresentation(result.primaryTarget, "home", "/home, config/routes.yaml")
        assertContainsUsageFile(result.usages, "templates/index.html.twig")
        assertContainsUsageFile(result.usages, "templates/secondary.html.twig")
    }

    fun testTwigPathFindUsagesTargetsXmlDeclaration() {
        myFixture.addFileToProject(
            "config/routes.xml",
            """
            <routes>
                <route id="home" path="/home" controller="App\Controller\HomeController::index"/>
            </routes>
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/secondary.html.twig", "{{ url('home') }}")

        val result = findUsagesFromTwigRouteUsage("templates/index.html.twig", "{{ path('ho<caret>me') }}")

        assertInstanceOf(result.targetElement, XmlAttribute::class.java)
        assertRouteTargetPresentation(result.primaryTarget, "home", "/home, config/routes.xml")
        assertContainsUsageFile(result.usages, "templates/index.html.twig")
        assertContainsUsageFile(result.usages, "templates/secondary.html.twig")
    }

    fun testPhpAttributeFindUsagesIncludesTwigTemplatePathUsage() {
        val psiFile = configureByProjectPath(
            "src/Controller/HomeController.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class HomeController {
                #[Route('/home', name: 'ho<caret>me')]
                public function index() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/index.html.twig", "{{ path('home') }}")
        myFixture.addFileToProject("templates/secondary.html.twig", "{{ url('home') }}")

        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val result = findUsagesFromDeclaration(element!!)

        assertRouteTargetPresentation(result.primaryTarget, "home", "/home, src/Controller/HomeController.php")
        assertContainsUsageFile(result.usages, "templates/index.html.twig")
        assertContainsUsageFile(result.usages, "templates/secondary.html.twig")
    }

    fun testClassLevelPhpAttributeFindUsagesIncludesTwigTemplatePathUsage() {
        val psiFile = configureByProjectPath(
            "src/Controller/HomeController.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            #[Route('/home', name: 'ho<caret>me')]
            class HomeController {
                public function __invoke() {}
            }
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/index.html.twig", "{{ path('home') }}")
        myFixture.addFileToProject("templates/secondary.html.twig", "{{ url('home') }}")

        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val result = findUsagesFromDeclaration(element!!)

        assertRouteTargetPresentation(result.primaryTarget, "home", "/home, src/Controller/HomeController.php")
        assertContainsUsageFile(result.usages, "templates/index.html.twig")
        assertContainsUsageFile(result.usages, "templates/secondary.html.twig")
    }

    private fun findUsagesFromTwigRouteUsage(filePath: String, content: String): FindUsagesResult {
        val psiFile = configureByProjectPath(filePath, content)
        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val targets: Array<UsageTarget> = RouteUsageTargetProvider().getTargets(element!!)
        assertEquals(1, targets.size)
        assertInstanceOf(targets[0], PsiElementUsageTarget::class.java)

        val targetElement = (targets[0] as PsiElementUsageTarget).element
        assertNotNull(targetElement)

        return findUsagesFromDeclaration(targetElement!!)
    }

    private fun findUsagesFromDeclaration(targetElement: PsiElement): FindUsagesResult {
        val handler = RouteFindUsagesHandlerFactory().createFindUsagesHandler(targetElement, false)
        assertNotNull(handler)

        val primaryTarget = handler!!.primaryElements[0] as NavigationItem
        val usages = collectUsages(handler)

        return FindUsagesResult(targetElement, primaryTarget, usages)
    }

    private fun configureByProjectPath(filePath: String, content: String): PsiFile {
        val caretOffset = content.indexOf("<caret>")
        assertTrue("Missing <caret> marker", caretOffset >= 0)

        myFixture.addFileToProject(filePath, content.replace("<caret>", ""))
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(filePath))
        myFixture.editor.caretModel.moveToOffset(caretOffset)

        return myFixture.file
    }

    private fun collectUsages(handler: FindUsagesHandler): List<UsageInfo> {
        val options: FindUsagesOptions = handler.findUsagesOptions
        options.searchScope = GlobalSearchScope.projectScope(project)
        options.isSearchForTextOccurrences = false
        options.isUsages = true

        val usages = mutableListOf<UsageInfo>()
        val completed = handler.processElementUsages(handler.primaryElements[0], { usages.add(it) }, options)
        assertTrue("Find usages processing was interrupted", completed)

        return usages
    }

    private fun assertRouteTargetPresentation(navigationItem: NavigationItem, expectedName: String, expectedLocation: String) {
        assertNotNull(navigationItem.presentation)
        assertEquals(expectedName, navigationItem.presentation!!.presentableText)
        assertEquals(expectedLocation, navigationItem.presentation!!.locationString)
    }

    private fun assertContainsUsageFile(usages: List<UsageInfo>, relativePath: String) {
        val actualUsages = usages.map { it.virtualFile?.path ?: "<no-path>" }
        if (usages.any { it.virtualFile?.path?.endsWith(relativePath) == true }) {
            return
        }

        fail("Expected usage in file: $relativePath; actual: $actualUsages")
    }

    private data class FindUsagesResult(
        val targetElement: PsiElement,
        val primaryTarget: NavigationItem,
        val usages: List<UsageInfo>,
    )
}
