package fr.adrienbrault.idea.symfony2plugin.tests.integrations.database

import fr.adrienbrault.idea.symfony2plugin.integrations.database.DoctrineEntityTableNameResolver
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

class DoctrineEntityTableNameResolverTest : SymfonyLightCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.xml"))
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.yml"))
    }

    override fun getTestDataPath(): String =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/integrations/database/fixtures"

    fun testTableNameResolutionFromAttribute() {
        val phpClass = PhpElementsUtil.getClassInterface(project, "Entity\\AttributeUser")
        assertNotNull(phpClass)

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(phpClass!!, "attribute_users"))
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "attribute_user"))
    }

    fun testTableNameResolutionFromAnnotation() {
        val phpClass = PhpElementsUtil.getClassInterface(project, "Entity\\AnnotatedUser")
        assertNotNull(phpClass)

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(phpClass!!, "annotated_users"))
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "annotated_user"))
    }

    fun testTableNameResolutionFromXmlMapping() {
        val phpClass = PhpElementsUtil.getClassInterface(project, "Entity\\XmlMappedUser")
        assertNotNull(phpClass)

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(phpClass!!, "xml_users"))
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "xml_user"))
    }

    fun testTableNameResolutionFromYamlMapping() {
        val phpClass = PhpElementsUtil.getClassInterface(project, "Entity\\YamlMappedUser")
        assertNotNull(phpClass)

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(phpClass!!, "yaml_users"))
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(phpClass, "yaml_user"))
    }

    fun testGuessedTableNameFromClassName() {
        val blogPostClass = PhpElementsUtil.getClassInterface(project, "Entity\\BlogPost")
        assertNotNull(blogPostClass)

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(blogPostClass!!, "blog_posts"))
        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(blogPostClass, "blog_post"))
    }

    fun testGuessedTableNameFromUpperCamelCaseClassName() {
        val fooBarClass = PhpElementsUtil.getClassInterface(project, "Entity\\FooBar")
        assertNotNull(fooBarClass)

        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass!!, "foo_bar"))
        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "foo_bars"))
        // case-insensitive normalization
        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "FOO_BAR"))
        assertTrue(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "Foo_Bar"))
        assertFalse(DoctrineEntityTableNameResolver.isTableMatch(fooBarClass, "foobar"))
    }
}
