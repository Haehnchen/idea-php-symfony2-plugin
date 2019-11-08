package fr.adrienbrault.idea.symfonyplugin.tests.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfonyplugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfonyplugin.stubs.indexes.FileResourcesIndex;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.stubs.indexes.FileResourcesIndex
 */
public class FileResourcesIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(YAMLFileType.YML, "" +
                "app:\n" +
                "  resource: \"@AppBundle/Controller/\"\n" +
                "  prefix: \"/foo\"\n" +
                "\n" +
                "app1:\n" +
                "  resource: \"@AcmeOtherBundle/Resources/config/routing1.yml\"\n" +
                "app2:\n" +
                "  resource: '@AcmeOtherBundle/Resources/config/routing2.yml'\n" +
                "app3:\n" +
                "  resource: '@AcmeOtherBundle/Resources/config/routing3.yml'\n" +
                "app4:\n" +
                "  resource: '@AcmeOtherBundle///Resources/config\\\\\\routing4.yml'\n"
        );

        myFixture.configureByText(XmlFileType.INSTANCE, "" +
                "<routes>\n" +
                "    <import resource=\"@AcmeOtherBundle/Resources/config/routing.xml\" prefix=\"/foo2\"/>\n" +
                "    <import resource=\"@AcmeOtherBundle//Resources/config/routing1.xml\" />\n" +
                "    <import resource=\"@AcmeOtherBundle\\\\\\Resources/config///routing2.xml\" />\n" +
                "</routes>"
        );

    }

    public void testYamlResourcesImport() {
        assertIndexContains(FileResourcesIndex.KEY, "@AppBundle/Controller");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing1.yml");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing2.yml");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing3.yml");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing4.yml");
    }

    public void testXmlResourcesImport() {
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing.xml");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing1.xml");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing2.xml");
    }

    public void testIndexValue() {
        FileResource item = ContainerUtil.getFirstItem(FileBasedIndex.getInstance().getValues(FileResourcesIndex.KEY, "@AppBundle/Controller", GlobalSearchScope.allScope(getProject())));
        assertEquals("/foo", item.getPrefix());

        item = ContainerUtil.getFirstItem(FileBasedIndex.getInstance().getValues(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing.xml", GlobalSearchScope.allScope(getProject())));
        assertEquals("/foo2", item.getPrefix());
    }
}
