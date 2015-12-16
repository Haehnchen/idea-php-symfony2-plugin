package fr.adrienbrault.idea.symfony2plugin.tests.form.util;

import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClassEnum;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil
 */
public class FormOptionsUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("FormOptionsUtil.php");
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see FormOptionsUtil#getExtendedTypeClasses
     */
    public void testGetExtendedTypeClassesAsStringValue() {
        FormClass foo = ContainerUtil.getFirstItem(FormOptionsUtil.getExtendedTypeClasses(getProject(), "foo"));

        assertNotNull(foo);
        assertEquals(FormClassEnum.EXTENSION, foo.getType());
        assertEquals("\\Foo\\Bar\\MyType", foo.getPhpClass().getFQN());
    }

    /**
     * @see FormOptionsUtil#getExtendedTypeClasses
     */
    public void testGetExtendedTypeClassesAsClassConstant() {
        for (String s : new String[]{"Foo\\Bar\\MyType", "\\Foo\\Bar\\MyType"}) {
            FormClass myType = ContainerUtil.getFirstItem(FormOptionsUtil.getExtendedTypeClasses(getProject(), s));

            assertNotNull(myType);
            assertEquals(FormClassEnum.EXTENSION, myType.getType());
            assertEquals("\\Foo\\Bar\\BarType", myType.getPhpClass().getFQN());
        }
    }
}
