package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlGoToDeclarationHandler
 */
public class YamlGoToDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testGlobalServiceName() {
        assertNavigationMatch(YAMLFileType.YML, "bar: f<caret>oo", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: [ f<caret>oo ]", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: { f<caret>oo }", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar:\n  - f<caret>oo", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: [ bar, f<caret>oo ]", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: { bar: f<caret>oo }", getClassPattern());

        assertNavigationIsEmpty(YAMLFileType.YML, "fo<caret>o:\n  - foo");
        assertNavigationIsEmpty(YAMLFileType.YML, "bar: { f<caret>oo: bar }");

        assertNavigationMatch(YAMLFileType.YML, "bar: foo.ba<caret>r_foo", getClassPattern());
        assertNavigationIsEmpty(YAMLFileType.YML, "bar: foo.<caret>bar-foo");
    }

    public void testGlobalServiceNameQuote() {
        assertNavigationMatch(YAMLFileType.YML, "bar: 'f<caret>oo'", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: [ 'f<caret>oo' ]", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: { 'f<caret>oo' }", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar:\n  - 'f<caret>oo'", getClassPattern());
        assertNavigationIsEmpty(YAMLFileType.YML, "'fo<caret>o':\n  - foo");

        assertNavigationMatch(YAMLFileType.YML, "bar: \"f<caret>oo\"", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: [ \"f<caret>oo\" ]", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: { \"f<caret>oo\" }", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar:\n  - \"f<caret>oo\"", getClassPattern());
        assertNavigationIsEmpty(YAMLFileType.YML, "\"fo<caret>o\":\n  - foo");

    }

    public void testSpecialCharPrefix() {
        assertNavigationMatch(YAMLFileType.YML, "bar: @f<caret>oo", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: @?f<caret>oo=", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: @?f<caret>oo", getClassPattern());
    }

    public void testSpecialCharPrefixQuote() {
        assertNavigationMatch(YAMLFileType.YML, "bar: '@f<caret>oo'", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: '@?f<caret>oo='", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: '@?f<caret>oo'", getClassPattern());

        assertNavigationMatch(YAMLFileType.YML, "bar: \"@f<caret>oo\"", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: \"@?f<caret>oo=\"", getClassPattern());
        assertNavigationMatch(YAMLFileType.YML, "bar: \"@?f<caret>oo\"", getClassPattern());
    }

    public void testParameter() {
        assertNavigationMatch(YAMLFileType.YML, "bar: %foo_p<caret>arameter%");
    }

    @NotNull
    private PsiElementPattern.Capture<PhpClass> getClassPattern() {
        return PlatformPatterns.psiElement(PhpClass.class);
    }
}
