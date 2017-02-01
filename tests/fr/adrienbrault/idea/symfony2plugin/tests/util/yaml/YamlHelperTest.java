package fr.adrienbrault.idea.symfony2plugin.tests.util.yaml;


import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
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
        assertFalse(YamlHelper.isClassServiceId("foobar"));
        assertFalse(YamlHelper.isClassServiceId("foo.bar"));
        assertTrue(YamlHelper.isClassServiceId("foo\\bar"));
    }
}
