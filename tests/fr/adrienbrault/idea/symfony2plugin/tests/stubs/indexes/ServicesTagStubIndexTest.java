package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesTagStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesTagStubIndex
 */
public class ServicesTagStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("tagged.services.xml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("tagged.services.yml"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testTaggedServiceInIndex() {
        assertIndexContains(ServicesTagStubIndex.KEY, "foo.tagged.xml_type", "foo.tagged.yaml_type");
    }

    public void testTaggedServiceValueInIndex() {
        assertIndexContainsKeyWithValue(ServicesTagStubIndex.KEY, "foo.tagged.xml_type", new MyStringContainsAssert("xml_type_tag"));
        assertIndexContainsKeyWithValue(ServicesTagStubIndex.KEY, "foo.tagged.yaml_type", new MyStringContainsAssert("yaml_type_tag"));
    }

    private static class MyStringContainsAssert implements IndexValue.Assert<String[]> {
        @NotNull
        private final String find;

        public MyStringContainsAssert(@NotNull String find) {
            this.find = find;
        }

        @Override
        public boolean match(@NotNull String[] value) {
            return ArrayUtils.contains(value, this.find);
        }
    }
}
