package fr.adrienbrault.idea.symfony2plugin.tests.action.naming;

import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.JavascriptServiceNameStrategy;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.ServiceNameStrategyParameter;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class JavascriptServiceNameStrategyTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/action/naming/fixtures";
    }

    public void testGetServiceName() {
        if (true) {
            // TODO: migrate to org.graalvm.js => "graal.js". javascript engine removed since java 11
            return;
        }

        JavascriptServiceNameStrategy defaultNaming = new JavascriptServiceNameStrategy();

        Settings.getInstance(getProject()).serviceJsNameStrategy = "return args.projectName;";
        assertTrue(defaultNaming.getServiceName(new ServiceNameStrategyParameter(getProject(), "asas")).contains("GetServiceName"));

        Settings.getInstance(getProject()).serviceJsNameStrategy = "return args.className.replace(\"Foo\", \"Bar\");";
        assertEquals("Bar\\Class", defaultNaming.getServiceName(getParameter("Foo\\Class")));

        Settings.getInstance(getProject()).serviceJsNameStrategy = "return args.absolutePath;";
        assertTrue("Bar\\Class", defaultNaming.getServiceName(getParameter("MyClass\\Is\\Nice\\Nicer")).endsWith("classes.php"));

        Settings.getInstance(getProject()).serviceJsNameStrategy = "return null";
        assertNull(defaultNaming.getServiceName(getParameter("MyClass\\Is\\Nice\\Nicer")));

        Settings.getInstance(getProject()).serviceJsNameStrategy = "return args.defaultNaming;";
        assertEquals("my_class.is_nice.nicer", defaultNaming.getServiceName(getParameter("MyClassBundle\\Is\\Nice\\Nicer")));

        Settings.getInstance(getProject()).serviceJsNameStrategy = null;
    }

    private ServiceNameStrategyParameter getParameter(String className) {
        return new ServiceNameStrategyParameter(getProject(), className);
    }

}
