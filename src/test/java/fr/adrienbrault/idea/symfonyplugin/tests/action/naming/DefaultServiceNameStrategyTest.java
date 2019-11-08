package fr.adrienbrault.idea.symfonyplugin.tests.action.naming;

import fr.adrienbrault.idea.symfonyplugin.action.generator.naming.DefaultServiceNameStrategy;
import fr.adrienbrault.idea.symfonyplugin.action.generator.naming.ServiceNameStrategyParameter;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DefaultServiceNameStrategyTest extends SymfonyLightCodeInsightFixtureTestCase {

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

    public void testThatOnlyBundleInSubNamespaceShouldBeStripped() {
        DefaultServiceNameStrategy defaultNaming = new DefaultServiceNameStrategy();
        assertEquals("foobar_foo_bar_foo_bar_bundle.foo_bar.foo_bar", defaultNaming.getServiceName(getParameter("Foobar\\FooBar\\FooBar\\Bundle\\FooBar\\FooBar")));
        assertEquals("foobar_foo_bar_foo_bar_fo.foo_bar.foo_bar", defaultNaming.getServiceName(getParameter("Foobar\\FooBar\\FooBar\\FoBundle\\FooBar\\FooBar")));
    }

    public void testThatServiceNotInBundleCanContainBundleWord() {
        DefaultServiceNameStrategy defaultNaming = new DefaultServiceNameStrategy();
        assertEquals("foo.foo_bar.search_bundle_subscriber", defaultNaming.getServiceName(getParameter("Foo\\FooBar\\SearchBundleSubscriber")));
    }

    private ServiceNameStrategyParameter getParameter(String className) {
        return new ServiceNameStrategyParameter(getProject(), className);
    }

}
