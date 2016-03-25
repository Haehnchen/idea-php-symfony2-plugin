package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
 */
public class YamlElementPatternHelperTest extends SymfonyLightCodeInsightFixtureTestCase {

    private String[] dataProviders() {
        return new String[] {
            "   <caret>a\n",
            "   '<caret>'\n",
            "   \"<caret>\"\n",
            "   <caret>a: foo\n",
            "   '<caret>': foo\n",
            "   \"<caret>\": foo\n",
        };
    }

    public void testGetOrmRoot() {
        for (String s : dataProviders()) {
            assertTrue(YamlElementPatternHelper.getOrmRoot().accepts(createCaretElement(
                "class\\Foo:\n" + s, "foo.orm.yml"))
            );

            assertFalse(YamlElementPatternHelper.getOrmRoot().accepts(createCaretElement(
                "class\\Foo:\n" + s, "foo.aaa.yml"))
            );
        }
    }

    public void testGetWithFirstRootKey() {
        for (String s : dataProviders()) {
            assertTrue(YamlElementPatternHelper.getWithFirstRootKey().accepts(createCaretElement(
                "class\\Foo:\n" + s, "foo.orm.yml"))
            );
        }
    }

    public void testGetParentKeyName() {
        for (String s : dataProviders()) {
            assertTrue(YamlElementPatternHelper.getParentKeyName("requirements").accepts(createCaretElement(
                "requirements:\n" + s, "foo.orm.yml"))
            );
        }
    }

    public void testGetSingleLineScalarKey() {
        assertTrue(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(createCaretElement(
            "class: <caret>a"
        )));

        assertTrue(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(createCaretElement(
            "class: '<caret>'"
        )));

        assertTrue(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(createCaretElement(
            "class: \"<caret>\""
        )));
    }

    public void testGetOrmParentLookup() {
        for (String s : dataProviders()) {
            assertTrue(YamlElementPatternHelper.getOrmParentLookup("requirements").accepts(createCaretElement(
                "requirements:\n" + s, "foo.orm.yml"))
            );
        }
    }

    public void testGetFilterOnPrevParent() {
        assertTrue(YamlElementPatternHelper.getFilterOnPrevParent("services").accepts(createCaretElement(
            "services:\n" +
            "   foo:\n" +
            "       <caret>a: foo\n"
            , "foo.orm.yml"))
        );
    }

    public void testGetInsideKeyValue() {
        assertTrue(YamlElementPatternHelper.getInsideKeyValue("tags").accepts(createCaretElement(
            "services:\n" +
                "   foo:\n" +
                "       tags:\n" +
                "         - { name: <caret>}\n"
            ))
        );

        assertTrue(YamlElementPatternHelper.getInsideKeyValue("services").accepts(createCaretElement(
            "services:\n" +
                "   foo:\n" +
                "       tags:\n" +
                "         - { name: <caret>}\n"
            ))
        );
    }

    public void testGetSuperParentArrayKey() {
        assertTrue(YamlElementPatternHelper.getSuperParentArrayKey("services").accepts(createCaretElement(
            "services:\n" +
            "   foo:\n" +
            "       <caret>a\n" +
            "       car: foo\n"
        )));

        // @TODO: PhpStorm <= 9 drop
        int baselineVersion = ApplicationInfo.getInstance().getBuild().getBaselineVersion();
        if(baselineVersion < 143) {
            System.out.println("Skipping testGetSuperParentArrayKey for wrong baseline version");
            return;
        }

        assertTrue(YamlElementPatternHelper.getSuperParentArrayKey("services").accepts(createCaretElement(
            "services:\n" +
                "   foo:\n" +
                "       \"<caret>\"\n" +
                "       car: foo\n"
        )));

        assertTrue(YamlElementPatternHelper.getSuperParentArrayKey("services").accepts(createCaretElement(
            "services:\n" +
                "   foo:\n" +
                "       '<caret>'\n" +
                "       car: foo\n"
        )));

        assertTrue(YamlElementPatternHelper.getSuperParentArrayKey("services").accepts(createCaretElement(
            "services:\n" +
                "   foo:\n" +
                "       <caret>a: foo\n"
        )));

        assertTrue(YamlElementPatternHelper.getSuperParentArrayKey("services").accepts(createCaretElement(
            "services:\n" +
                "   foo:\n" +
                "       car: foo\n" +
                "       <caret>a"
        )));

        assertFalse(YamlElementPatternHelper.getSuperParentArrayKey("services").accepts(createCaretElement(
            "service:\n" +
                "   foo:\n" +
                "       car: foo\n" +
                "       <caret>a"
        )));
    }

    private PsiElement createCaretElement(@NotNull String contents) {
        return createCaretElement(contents, null);
    }

    private PsiElement createCaretElement(@NotNull String contents, @Nullable String file) {
        if(file == null) {
            file = "services.yml";
        }

        myFixture.configureByText(file, contents);

        return myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    }

}
