package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineYamlGotoCompletionRegistrar
 */
public class DoctrineYamlGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testRepositoryClass() {

        Collection<String[]> providers = new ArrayList<String[]>() {{
            add(new String[] {"orm", "repositoryClass"});
            add(new String[] {"odm", "repositoryClass"});
            add(new String[] {"couchdb", "repositoryClass"});
            add(new String[] {"mongodb", "repositoryClass"});
        }};

        for (String[] provider : providers) {
            assertNavigationMatch(
                String.format("foo.%s.yml", provider[0]),
                String.format("Foo\\Bar\\Ns\\Bar:\n  %s: Bar<caret>Repo", provider[1]),
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertNavigationMatch(
                String.format("foo.%s.yml", provider[0]),
                String.format("Foo\\Bar\\Ns\\Bar:\n  %s: Foo\\Bar\\N<caret>s\\BarRepo", provider[1]),
                PlatformPatterns.psiElement(PhpClass.class)
            );

            assertCompletionContains(
                String.format("foo.%s.yml", provider[0]),
                String.format("Foo\\Bar\\Ns\\Bar:\n  %s: <caret>", provider[1]),
                "Foo\\Bar\\Ns\\BarRepo"
            );
        }
    }
}
