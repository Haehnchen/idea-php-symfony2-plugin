package fr.adrienbrault.idea.symfony2plugin.tests.completion;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.completion.PhpGotoDeclarationCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see PhpGotoDeclarationCompletionContributor
 */
public class PhpGotoDeclarationCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("routes.yml");
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/fixtures";
    }

    public void testCompletionForHttpClientOptions() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var \\Symfony\\Contracts\\HttpClient\\HttpClientInterface $foobar */\n" +
                "$foobar->request('', '', ['<caret>']);",
            "auth_basic", "json"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var \\Symfony\\Contracts\\HttpClient\\HttpClientInterface $foobar */\n" +
                "$foobar->request('', '', ['<caret>' => '']);",
            "auth_basic", "json"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var \\Symfony\\Contracts\\HttpClient\\HttpClientInterface $foobar */\n" +
                "$foobar->withOptions(['<caret>']);",
            "auth_basic", "json"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var \\Symfony\\Contracts\\HttpClient\\HttpClientInterface $foobar */\n" +
                "$foobar->withOptions(['<caret>' => '']);",
            "auth_basic", "json"
        );
    }

    public void testNavigationForHttpClientOptions() {
        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var \\Symfony\\Contracts\\HttpClient\\HttpClientInterface $foobar */\n" +
                "$foobar->request('', '', ['auth<caret>_basic' => '']);",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var \\Symfony\\Contracts\\HttpClient\\HttpClientInterface $foobar */\n" +
                "$foobar->withOptions(['auth<caret>_basic' => '']);",
            PlatformPatterns.psiElement()
        );
    }

    public void testPartialNavigationForPath() {
        PsiElementPattern.Capture<PsiElement> with = PlatformPatterns.psiElement().with(new PatternCondition<>("match") {
            @Override
            public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext context) {
                String text = psiElement.getText();
                return text.contains("path: '/test/foobar/car/foobar'\n");
            }
        });

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\Controller;" +
                "" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    #[Route('/test/foobar/c<caret>ar/blub')]\n" +
                "    public function foo1() {}\n" +
                "}",
            with
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\Controller;" +
                "" +
                "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
                "\n" +
                "class FooController\n" +
                "{\n" +
                "    /**\n" +
                "     * @Route(\"/test/foobar/c<caret>ar/blub\")\n" +
                "     */\n" +
                "    public function foo1() {}\n" +
                "}",
            with
        );
    }

    public void testPartialNavigationForItselfShouldbeEmpty() {
        assertNavigationIsEmpty(PhpFileType.INSTANCE, "<?php\n" + "" +
            "namespace App\\Controller;" +
            "" +
            "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
            "\n" +
            "class FooController\n" +
            "{\n" +
            "    #[Route('/test/foobar/car/bl<caret>ub')]" +
            "    public function foo1() {}\n" +
            "}"
        );
    }
}
