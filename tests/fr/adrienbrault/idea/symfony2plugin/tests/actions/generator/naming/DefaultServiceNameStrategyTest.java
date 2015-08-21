package fr.adrienbrault.idea.symfony2plugin.tests.actions.generator.naming;

import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.DefaultServiceNameStrategy;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.ServiceNameStrategyParameter;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */

public class DefaultServiceNameStrategyTest  extends SymfonyLightCodeInsightFixtureTestCase {

    public void testGetServiceName() {

        DefaultServiceNameStrategy defaultNaming = new DefaultServiceNameStrategy();

        assertEquals("foo.foo_name.class_name", defaultNaming.getServiceName(getParameter("FooBundle\\FooName\\ClassName")));
        assertEquals("foo.form_bar.class_name_form", defaultNaming.getServiceName(getParameter("FooBundle\\Form\\Bar\\ClassNameForm")));
        assertEquals("foo.form.bar.class_name_form", defaultNaming.getServiceName(getParameter("Foo\\Form\\Bar\\ClassNameForm")));

        assertEquals("foo_form_bar.class_name_form", defaultNaming.getServiceName(getParameter("Foo\\Form\\BarBundle\\ClassNameForm")));
        assertEquals("foo.form.bar.class_name_form", defaultNaming.getServiceName(getParameter("Foo\\Form\\Bar\\ClassNameForm")));

        assertEquals("foo", defaultNaming.getServiceName(getParameter("\\Foo")));
        assertEquals("foo", defaultNaming.getServiceName(getParameter("\\FooBundle")));
        assertEquals("foo.foo", defaultNaming.getServiceName(getParameter("\\FooBundle\\Foo")));
        assertEquals("foo", defaultNaming.getServiceName(getParameter("\\FooBundle\\")));
    }

    private ServiceNameStrategyParameter getParameter(String className) {
        return new ServiceNameStrategyParameter(getProject(), className);
    }

}
