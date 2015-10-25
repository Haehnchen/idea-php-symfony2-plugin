package fr.adrienbrault.idea.symfony2plugin.tests.doctrine;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
        assertEquals("FooBundle\\BarRepository", EntityHelper.getEntityRepositoryClass(getProject(), "FooBundle:Bar").getPresentableFQN());
        assertEquals("FooBundle\\BarRepository", EntityHelper.getEntityRepositoryClass(getProject(), "FooBundle\\Entity\\Bar").getPresentableFQN());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper#getEntityRepositoryClass
     */
    public void testGetEntityRepositoryClassInSameNamespaceFallback() {
        assertEquals("FooBundle\\Entity\\Car\\BarRepository", EntityHelper.getEntityRepositoryClass(getProject(), "FooBundle:Car\\Bar").getPresentableFQN());
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

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper#getModelClasses
     */
    public void testGetModelClasses() {
        Collection<DoctrineModel> modelClasses = EntityHelper.getModelClasses(getProject());

        Map<String, String> map = new HashMap<String, String>();
        for (DoctrineModel modelClass : modelClasses) {
            map.put(modelClass.getRepositoryName(), modelClass.getDoctrineNamespace());
        }

        assertContainsElements(map.keySet(), "FooBundle:Doc");
        assertContainsElements(map.keySet(), "FooBundle\\Entity\\Bar");
        assertContainsElements(map.values(), "\\FooBundle\\Document");

        assertFalse(map.values().contains("FooBundle:DocBarRepository"));
    }
}
