package fr.adrienbrault.idea.symfonyplugin.tests.util.yaml;


import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.junit.Assert;
import org.junit.Test;

public class YamlHelperTest extends Assert {

    @Test
    public void testIsValidParameterName() {
        assertTrue(YamlHelper.isValidParameterName("%a%"));
        assertTrue(YamlHelper.isValidParameterName("%a.a%"));
        assertTrue(YamlHelper.isValidParameterName("%a.-_a%"));

        assertFalse(YamlHelper.isValidParameterName("%%"));
        assertFalse(YamlHelper.isValidParameterName(""));
        assertFalse(YamlHelper.isValidParameterName("%"));
        assertFalse(YamlHelper.isValidParameterName("%kernel.root_dir%/../web/"));
        assertFalse(YamlHelper.isValidParameterName("%kernel.root_dir%/../web/%"));
        assertFalse(YamlHelper.isValidParameterName("%kernel.root_dir%/../web/%webpath_modelmasks%"));
        assertFalse(YamlHelper.isValidParameterName("%env(FO<caret>O)"));
        assertFalse(YamlHelper.isValidParameterName("%ENV(FO<caret>O)"));
    }

    @Test
    public void testTrimSpecialSyntaxServiceName() {
        assertEquals("logger", YamlHelper.trimSpecialSyntaxServiceName("@?logger="));
        assertEquals("logger", YamlHelper.trimSpecialSyntaxServiceName("?logger="));
        assertEquals("logger", YamlHelper.trimSpecialSyntaxServiceName("?logger"));
        assertEquals("logger", YamlHelper.trimSpecialSyntaxServiceName("logger="));
        assertEquals("", YamlHelper.trimSpecialSyntaxServiceName("?"));
        assertEquals("", YamlHelper.trimSpecialSyntaxServiceName("="));
    }

    @Test
    public void testIsClassServiceId() {
        assertFalse(YamlHelper.isClassServiceId("foo.bar"));
        assertFalse(YamlHelper.isClassServiceId("foo-bar"));
        assertFalse(YamlHelper.isClassServiceId("Foo\\Bar-Car"));

        assertTrue(YamlHelper.isClassServiceId("foobar"));
        assertTrue(YamlHelper.isClassServiceId("foo\\bar"));
        assertTrue(YamlHelper.isClassServiceId("\\Foo\\Bar\\Bar"));
        assertTrue(YamlHelper.isClassServiceId("FooBar"));
        assertTrue(YamlHelper.isClassServiceId("My\\Sweet\\Foobar"));

        // reserved "Class" keyword
        assertFalse(YamlHelper.isClassServiceId("My\\Sweet\\Class"));
    }
}
