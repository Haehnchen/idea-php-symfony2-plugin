package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResourceContextTypeEnum;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FileResourcesIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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

        myFixture.configureByText("test.yml", "" +
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
                "  resource: '@AcmeOtherBundle///Resources/config\\\\\\routing4.yml'\n" +
                "imports:\n" +
                "  - { resource: ../src/import/services.yml, ignore_errors: true }\n" +
                "web_profiler_wdt_1:\n" +
                "    resource: '@WebProfilerBundle/Resources/config/routing/wdt_1.xml'\n" +
                "    prefix: /_wdt\n" +
                "when@dev:\n" +
                "    web_profiler_wdt_2:\n" +
                "        resource: '@WebProfilerBundle/Resources/config/routing/wdt_2.xml'\n" +
                "        prefix: /_wdt\n"
        );

        myFixture.configureByText("test1.xml", "" +
                "<routes>\n" +
                "    <import resource=\"@AcmeOtherBundle/Resources/config/routing.xml\" type=\"annotation\" prefix=\"/foo\" name-prefix=\"blog_\"/>\n" +
                "    <import resource=\"@AcmeOtherBundle//Resources/config/routing1.xml\" />\n" +
                "    <import resource=\"@AcmeOtherBundle\\\\\\Resources/config///routing2.xml\" />\n" +
                "</routes>"
        );

        myFixture.configureByText("test2.xml", "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<container>\n" +
            "    <imports>\n" +
            "        <import resource=\"%kernel.project_dir%/somefile.yaml\"/>\n" +
            "    </imports>\n" +
            "</container>"
        );
    }

    public void testYamlResourcesImport() {
        assertIndexContains(FileResourcesIndex.KEY, "@AppBundle/Controller");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing1.yml");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing2.yml");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing3.yml");
        assertIndexContains(FileResourcesIndex.KEY, "@AcmeOtherBundle/Resources/config/routing4.yml");

        assertIndexContains(FileResourcesIndex.KEY, "../src/import/services.yml");
        assertIndexContains(FileResourcesIndex.KEY, "../src/import/services.yml");

        assertIndexContains(FileResourcesIndex.KEY, "@WebProfilerBundle/Resources/config/routing/wdt_1.xml");
        assertIndexContains(FileResourcesIndex.KEY, "@WebProfilerBundle/Resources/config/routing/wdt_2.xml");
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
