package fr.adrienbrault.idea.symfony2plugin.tests.routing.usages

import com.intellij.psi.JavaPsiFacade
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTarget
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteUsageTargetProvider
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class RouteUsageTargetProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testProvidesUsageTargetOnPhpRouteUsage() {
        myFixture.configureByText(
            "route.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class MyController {
                #[Route('/foo', name: 'app_report_submitter')]
                public function __invoke() {}
            }
            """.trimIndent()
        )

        val psiFile = myFixture.configureByText("usage.php", "<?php\n\$this->generateUrl('app_report_submitter<caret>');")
        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val targets: Array<UsageTarget> = RouteUsageTargetProvider().getTargets(element!!)
        assertEquals(1, targets.size)
        assertTrue(targets[0].isValid)
        assertInstanceOf(targets[0], PsiElementUsageTarget::class.java)
        assertInstanceOf((targets[0] as PsiElementUsageTarget).element, StringLiteralExpression::class.java)
    }

    fun testProvidesUsageTargetOnTwigRouteUsage() {
        myFixture.configureByText(
            "route.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class MyController {
                #[Route('/foo', name: 'app_report_submitter')]
                public function __invoke() {}
            }
            """.trimIndent()
        )

        val psiFile = myFixture.configureByText("template.html.twig", "{{ path('app_report_submitter<caret>') }}")
        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val targets: Array<UsageTarget> = RouteUsageTargetProvider().getTargets(element!!)
        assertEquals(1, targets.size)
        assertTrue(targets[0].isValid)
    }

    fun testProvidesUsageTargetOnPhpRouteDeclarationWithDefaults() {
        val psiFile = myFixture.configureByText(
            "route.php",
            """
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class MyController {
                #[Route('/foo', name: 'app_report_submitter<caret>', defaults: ['_firewall' => true])]
                public function __invoke() {}
            }
            """.trimIndent()
        )

        val element = psiFile.findElementAt(myFixture.caretOffset)
        assertNotNull(element)

        val targets: Array<UsageTarget> = RouteUsageTargetProvider().getTargets(element!!)
        assertEquals(1, targets.size)
        assertTrue(targets[0].isValid)
    }

    fun testReturnsNoTargetsForPsiPackage() {
        myFixture.addFileToProject(
            "foo/Bar.java",
            """
            package foo;
            class Bar {}
            """.trimIndent()
        )

        val psiPackage = JavaPsiFacade.getInstance(project).findPackage("foo")
        assertNotNull(psiPackage)

        val targets: Array<UsageTarget> = RouteUsageTargetProvider().getTargets(psiPackage!!)
        assertEquals(0, targets.size)
    }
}
