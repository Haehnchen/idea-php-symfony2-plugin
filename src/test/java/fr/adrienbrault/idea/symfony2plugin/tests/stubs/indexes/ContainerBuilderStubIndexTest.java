package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerBuilderStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerBuilderStubIndex
 */
public class ContainerBuilderStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("ContainerBuilder.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatMethodsAreFound() {
        assertIndexContains(ContainerBuilderStubIndex.KEY, "setParameter");
        assertIndexContains(ContainerBuilderStubIndex.KEY, "findTaggedServiceIds");
        assertIndexContains(ContainerBuilderStubIndex.KEY, "setDefinition");
        assertIndexContains(ContainerBuilderStubIndex.KEY, "setAlias");
    }

    public void testThatMethodParameterAreFound() {
        assertIndexContainsKeyWithValue(ContainerBuilderStubIndex.KEY, "setParameter", value ->
            value.getParameter().contains("parameter")
        );

        assertIndexContainsKeyWithValue(ContainerBuilderStubIndex.KEY, "findTaggedServiceIds", value ->
            value.getParameter().contains("TaggedServiceIds") &&
            value.getParameter().contains("TaggedServiceIds2") &&
            value.getParameter().contains("TaggedServiceIds4")
        );

        assertIndexContainsKeyWithValue(ContainerBuilderStubIndex.KEY, "findTaggedServiceIds", value ->
            value.getParameter().contains("TaggedServiceIds4")
        );

        assertIndexContainsKeyWithValue(ContainerBuilderStubIndex.KEY, "setDefinition", value ->
            value.getParameter().contains("definition") && value.getParameter().contains("definition3")
        );

        assertIndexContainsKeyWithValue(ContainerBuilderStubIndex.KEY, "setAlias", value ->
            value.getParameter().contains("alias")
        );

        assertIndexContainsKeyWithValue(ContainerBuilderStubIndex.KEY, "setDefinition", value ->
            !value.getParameter().contains("setDefinition")
        );
    }

    public void testThatRegisterMethodIsIndexed() {
        assertIndexContainsKeyWithValue(ContainerBuilderStubIndex.KEY, "register", value ->
            value.getParameter().contains("register.id")
        );
    }
}
