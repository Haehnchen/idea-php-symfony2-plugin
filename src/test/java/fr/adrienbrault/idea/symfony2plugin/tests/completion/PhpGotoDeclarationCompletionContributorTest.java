package fr.adrienbrault.idea.symfony2plugin.tests.completion;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.completion.PhpGotoDeclarationCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see PhpGotoDeclarationCompletionContributor
 */
public class PhpGotoDeclarationCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
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
}
