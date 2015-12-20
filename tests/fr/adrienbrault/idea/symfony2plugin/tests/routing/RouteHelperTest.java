package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;

import java.io.File;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
 */
public class RouteHelperTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("RouteHelper.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getYamlRouteDefinitions
     */
    public void testGetYamlRouteDefinitionsAsHashAndKeyValue() {
        Collection<String[]> providers = new ArrayList<String[]>() {{
            add(new String[] {"'MyController::fooAction'", "MyController::fooAction"});
            add(new String[] {"MyController::fooAction", "MyController::fooAction"});
            add(new String[] {"\"MyController::fooAction\"", "MyController::fooAction"});
        }};

        for (String[] provider : providers) {
            Collection<YAMLDocument> yamlDocuments = new ArrayList<YAMLDocument>();

            yamlDocuments.add(YamlPsiElementFactory.createFromText(getProject(), YAMLDocument.class, String.format(
                    "route1:\n" +
                    "    path: /foo\n" +
                    "    defaults: { _controller: %s }",
                provider[1]
            )));

            yamlDocuments.add(YamlPsiElementFactory.createFromText(getProject(), YAMLDocument.class, String.format(
                "route1:\n" +
                    "    path: /foo\n" +
                    "    defaults:\n" +
                    "       _controller: %s",
                provider[1]
            )));

            yamlDocuments.add(YamlPsiElementFactory.createFromText(getProject(), YAMLDocument.class, String.format(
                "route1:\n" +
                    "    pattern: /foo\n" +
                    "    defaults:\n" +
                    "       _controller: %s",
                provider[1]
            )));

            for (YAMLDocument yamlDocument : yamlDocuments) {
                StubIndexedRoute route1 = ContainerUtil.find(RouteHelper.getYamlRouteDefinitions(yamlDocument), new MyEqualStubIndexedRouteCondition("route1"));
                assertNotNull(route1);

                assertEquals(provider[1], route1.getController());
                assertEquals("route1", route1.getName());
                assertEquals("/foo", route1.getPath());
            }
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getYamlRouteDefinitions
     */
    public void testGetYamlRouteDefinitionsWithMethods() {
        Collection<StubIndexedRoute> yamlRouteDefinitions = RouteHelper.getYamlRouteDefinitions(YamlPsiElementFactory.createFromText(getProject(), YAMLDocument.class,
            "route1:\n" +
            "    path: /foo\n" +
            "    methods: [GET]\n" +
            "route2:\n" +
            "    path: /foo\n" +
            "    methods: GET\n" +
            "route3:\n" +
            "    path: /foo\n" +
            "    methods: [GET,   POST]\n"
        ));

        assertContainsElements(Collections.singletonList("GET"), ContainerUtil.find(yamlRouteDefinitions, new MyEqualStubIndexedRouteCondition("route1")).getMethods());
        assertContainsElements(Collections.singletonList("GET"), ContainerUtil.find(yamlRouteDefinitions, new MyEqualStubIndexedRouteCondition("route2")).getMethods());
        assertContainsElements(Arrays.asList("GET", "POST"), ContainerUtil.find(yamlRouteDefinitions, new MyEqualStubIndexedRouteCondition("route3")).getMethods());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getXmlRouteDefinitions
     */
    public void testGetXmlRouteDefinitions() {
        Collection<XmlFile> xmlFiles = new ArrayList<XmlFile>();

        xmlFiles.add(createXmlFile("" +
                "<routes>\n" +
                "   <route id=\"foo2\" path=\"/blog/{slug}\">\n" +
                "       <default key=\"_controller\">MyController::fooAction</default>\n" +
                "   </route>\n" +
                "</routes>"
        ));

        xmlFiles.add(createXmlFile("" +
                "<routes>\n" +
                "   <route id=\"foo2\" pattern=\"/blog/{slug}\">\n" +
                "       <default key=\"_controller\">MyController::fooAction</default>\n" +
                "   </route>\n" +
                "</routes>"
        ));

        for (XmlFile xmlFile : xmlFiles) {
            StubIndexedRoute route1 = ContainerUtil.find(RouteHelper.getXmlRouteDefinitions(xmlFile), new MyEqualStubIndexedRouteCondition("foo2"));

            assertNotNull(route1);

            assertEquals("MyController::fooAction", route1.getController());
            assertEquals("foo2", route1.getName());
            assertEquals("/blog/{slug}", route1.getPath());
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getXmlRouteDefinitions
     */
    public void testGetXmlRouteDefinitionsWithoutController() {
        StubIndexedRoute route1 = ContainerUtil.find(RouteHelper.getXmlRouteDefinitions(createXmlFile("" +
                "<routes>\n" +
                "   <route id=\"foo2\" pattern=\"/blog/{slug}\"/>\n" +
                "</routes>"
        )), new MyEqualStubIndexedRouteCondition("foo2"));

        assertNotNull(route1);

        assertNull(route1.getController());
        assertEquals("foo2", route1.getName());
        assertEquals("/blog/{slug}", route1.getPath());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getXmlRouteDefinitions
     */
    public void testGetXmlRouteDefinitionsWithoutPath() {

        StubIndexedRoute route = ContainerUtil.find(RouteHelper.getXmlRouteDefinitions(createXmlFile("" +
                "<routes>\n" +
                "   <route id=\"foo2\"/>\n" +
                "</routes>"
        )), new MyEqualStubIndexedRouteCondition("foo2"));

        assertNotNull(route);

        assertNull(route.getController());
        assertEquals("foo2", route.getName());
        assertNull(route.getPath());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getXmlRouteDefinitions
     */
    public void testGetXmlRouteDefinitionsWithMethods() {
        Collection<StubIndexedRoute> xmlRouteDefinitions = RouteHelper.getXmlRouteDefinitions(createXmlFile("" +
                "<routes>\n" +
                "   <route id=\"foo2\" methods=\"GET\"/>\n" +
                "   <route id=\"foo3\" methods=\"GET   POST PUT   FIGHT\"/>\n" +
                "</routes>"
        ));

        assertContainsElements(
            Collections.singletonList("GET"),
            ContainerUtil.find(xmlRouteDefinitions, new MyEqualStubIndexedRouteCondition("foo2")).getMethods()
        );

        assertContainsElements(
            Arrays.asList("GET", "POST", "PUT", "FIGHT"),
            ContainerUtil.find(xmlRouteDefinitions, new MyEqualStubIndexedRouteCondition("foo3")).getMethods()
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getAllRoutes
     */
    public void testGetAllRoutes() {
        Map<String, Route> allRoutes = RouteHelper.getAllRoutes(getProject());

        assertEquals("my_foo_bar_car_index", allRoutes.get("my_foo_bar_car_index").getName());
        assertEquals("/foo_bar/edit/{id}", allRoutes.get("my_foo_bar_car_index").getPath());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getRoutesLookupElements
     */
    public void testGetRoutesLookupElements() {
        assertNotNull(ContainerUtil.find(RouteHelper.getRoutesLookupElements(getProject()), new Condition<LookupElement>() {
            @Override
            public boolean value(LookupElement lookupElement) {
                return "my_foo_bar_car_index".equals(lookupElement.getLookupString());
            }
        }));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper#getRoutesInsideUrlGeneratorFile
     */
    public void testGetRoutesInsideUrlGeneratorFile() {
        Map<String, Route> routes = RouteHelper.getRoutesInsideUrlGeneratorFile(getProject(), myFixture.copyFileToProject("appTestUrlGenerator.php"));
        assertEquals("Lol\\CoreBundle\\Controller\\FeedbackController::feedbackAction", routes.get("feedback").getController());
        assertEquals("Lol\\ApiBundle\\Controller\\UsersController::getInfoAction", routes.get("api_users_getInfo").getController());
        assertNull(routes.get("ru__RG__page"));
        assertNull(routes.get("_assetic_91dd2a8"));
    }

    @NotNull
    private XmlFile createXmlFile(@NotNull String content) {
        return (XmlFile) PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + XmlFileType.INSTANCE.getDefaultExtension(), XmlFileType.INSTANCE, content);
    }

    private static class MyEqualStubIndexedRouteCondition implements Condition<StubIndexedRoute> {

        @NotNull
        private final String routeName;

        public MyEqualStubIndexedRouteCondition(@NotNull String routeName) {
            this.routeName = routeName;
        }

        @Override
        public boolean value(StubIndexedRoute stubIndexedRoute) {
            return stubIndexedRoute.getName().equals(this.routeName);
        }
    }
}
