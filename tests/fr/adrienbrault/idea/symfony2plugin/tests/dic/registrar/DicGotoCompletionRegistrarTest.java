package fr.adrienbrault.idea.symfony2plugin.tests.dic.registrar;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DicGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.configureByText(YAMLFileType.YML, "parameters:\n  foo: foo");

    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testParameterContributor() {
        for (String s : new String[]{"getParameter", "hasParameter"}) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $f \\Symfony\\Component\\DependencyInjection\\ContainerInterface */ \n" +
                    String.format("$f->%s('<caret>')", s),
                "foo"
            );

            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $f \\Symfony\\Component\\DependencyInjection\\ContainerInterface */ \n" +
                    String.format("$f->%s('foo<caret>')", s),
                PlatformPatterns.psiElement()
            );
        }
    }

    public void testParameterContributorProxied() {
        for (String s : new String[]{"foo", "bar"}) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    String.format("(new \\Foo())->%s('<caret>')", s),
                "foo"
            );

            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $f \\Symfony\\Component\\DependencyInjection\\ContainerInterface */ \n" +
                    String.format("(new \\Foo())->%s('foo<caret>')", s),
                PlatformPatterns.psiElement()
            );
        }
    }
}
