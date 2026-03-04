package fr.adrienbrault.idea.symfony2plugin.tests.integrations.database;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DoctrineEntityTableNameResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

public class DoctrineEntityTableNameResolverTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.yml"));
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/integrations/database/fixtures";
    }

    public void testTableNameResolutionFromAttribute() {
        PhpClass phpClass = PhpElementsUtil.getClassInterface(getProject(), "Entity\\AttributeUser");
        assertNotNull(phpClass);

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "attribute_users"));
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "attribute_user"));
    }

    public void testTableNameResolutionFromAnnotation() {
        PhpClass phpClass = PhpElementsUtil.getClassInterface(getProject(), "Entity\\AnnotatedUser");
        assertNotNull(phpClass);

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "annotated_users"));
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "annotated_user"));
    }

    public void testTableNameResolutionFromXmlMapping() {
        PhpClass phpClass = PhpElementsUtil.getClassInterface(getProject(), "Entity\\XmlMappedUser");
        assertNotNull(phpClass);

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "xml_users"));
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "xml_user"));
    }

    public void testTableNameResolutionFromYamlMapping() {
        PhpClass phpClass = PhpElementsUtil.getClassInterface(getProject(), "Entity\\YamlMappedUser");
        assertNotNull(phpClass);

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "yaml_users"));
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "yaml_user"));
    }

    public void testGuessedTableNameFromClassName() {
        PhpClass blogPostClass = PhpElementsUtil.getClassInterface(getProject(), "Entity\\BlogPost");
        assertNotNull(blogPostClass);

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(blogPostClass, "blog_posts"));
        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(blogPostClass, "blog_post"));
    }

    public void testGuessedTableNameFromUpperCamelCaseClassName() {
        PhpClass fooBarClass = PhpElementsUtil.getClassInterface(getProject(), "Entity\\FooBar");
        assertNotNull(fooBarClass);

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "foo_bar"));
        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "foo_bars"));
        // case-insensitive normalization
        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "FOO_BAR"));
        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "Foo_Bar"));
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "foobar"));
    }

}
