package fr.adrienbrault.idea.symfony2plugin.tests.dic.suggestion;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.PhpAttributeServiceSuggestionCollector;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeServiceSuggestionCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.copyFileToProject("services.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/suggestion/fixtures";
    }

    public void testParameterContributor() {
        @Nullable StringLiteralExpression stringLiteralExpression = PhpPsiElementFactory.createFromText(getProject(), StringLiteralExpression.class,
            "<?php\n" +
            "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
            "\n" +
            "class HandlerCollection\n" +
            "{\n" +
            "    public function __construct(\n" +
            "        #[Autowire(service: '<caret>')] \\Foo\\Bar $test\n" +
            "    ) {}\n" +
            "}"
        );

        assert stringLiteralExpression != null;

        Collection<String> suggestions = new PhpAttributeServiceSuggestionCollector().collect(stringLiteralExpression, ContainerCollectionResolver.getServices(getProject()).values());
        assertContainsElements(suggestions, "foo_bar_service");
    }
}
