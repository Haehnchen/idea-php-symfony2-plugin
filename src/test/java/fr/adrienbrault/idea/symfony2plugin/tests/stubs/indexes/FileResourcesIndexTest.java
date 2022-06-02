package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResourceContextTypeEnum;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FileResourcesIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FileResourcesIndex
 */
public class FileResourcesIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(YAMLFileType.YML, "" +
                "app:\n" +
                "  resource: \"@AppBundle/Controller/\"\n" +
                "  type: annotation\n" +
                "  prefix: \"/foo\"\n" +
                "  name_prefix: 'blog_'\n" +
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
                "    <import resource=\"@AcmeOtherBundle/Resources/config/routing.xml\" type=\"annotation\" prefix=\"/foo\" name-prefix=\"blog_\"/>\n" +
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
        Map<String, String> treeMap = new TreeMap<>();
        treeMap.put("name_prefix", "blog_");
        treeMap.put("prefix", "/foo");
        treeMap.put("type", "annotation");

        FileResource item = ContainerUtil.getFirstItem(FileBasedIndex.getInstance().getValues(FileResourcesIndex.KEY, "@AppBundle/Controller", GlobalSearchScope.allScope(getProject())));
        assertEquals("/foo", item.getPrefix());
        assertEquals(treeMap, item.getContextValues());
        assertEquals(FileResourceContextTypeEnum.ROUTE, item.getContextType());

        item = ContainerUtil.getFirstItem(FileBasedIndex.getInstance().getValues(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing.xml", GlobalSearchScope.allScope(getProject())));
        assertEquals("/foo", item.getPrefix());
        assertEquals(FileResourceContextTypeEnum.ROUTE, item.getContextType());
        assertEquals(treeMap, item.getContextValues());
    }
}
