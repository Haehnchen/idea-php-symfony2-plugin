package fr.adrienbrault.idea.symfony2plugin.tests.doctrine;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper
 */
public class EntityHelperTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("entity_helper.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper#getEntityRepositoryClass
     */
    public void testGetEntityRepositoryClass() {
        //EntityHelper.getEntityRepositoryClass()
    }
    
    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper#resolveShortcutName
     */
    public void testResolveShortcutName() {
        assertEquals("FooBundle\\Entity\\Bar", EntityHelper.resolveShortcutName(getProject(), "FooBundle:Bar").getPresentableFQN());
        assertEquals("FooBundle\\Document\\Doc", EntityHelper.resolveShortcutName(getProject(), "FooBundle:Doc").getPresentableFQN());

        assertEquals("FooBundle\\Entity\\Car\\Bar", EntityHelper.resolveShortcutName(getProject(), "FooBundle:Car\\Bar").getPresentableFQN());

        assertEquals("FooBundle\\Entity\\Bar", EntityHelper.resolveShortcutName(getProject(), "FooBundle\\Entity\\Bar").getPresentableFQN());
        assertEquals("FooBundle\\Document\\Doc", EntityHelper.resolveShortcutName(getProject(), "FooBundle\\Document\\Doc").getPresentableFQN());
        assertEquals("FooBundle\\Entity\\Car\\Bar", EntityHelper.resolveShortcutName(getProject(), "FooBundle\\Entity\\Car\\Bar").getPresentableFQN());

        assertEquals("FooBundle\\Entity\\Bar", EntityHelper.resolveShortcutName(getProject(), "\\FooBundle\\Entity\\Bar").getPresentableFQN());
        assertEquals("FooBundle\\Document\\Doc", EntityHelper.resolveShortcutName(getProject(), "\\FooBundle\\Document\\Doc").getPresentableFQN());

        assertNull("FooBundle\\Document\\Doc", EntityHelper.resolveShortcutName(getProject(), "FooBundle:Bike"));
        assertNull("FooBundle\\Document\\Doc", EntityHelper.resolveShortcutName(getProject(), "BarCarBundle:Bar"));
    }
}
