package fr.adrienbrault.idea.symfony2plugin.tests.doctrine;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import org.jetbrains.yaml.psi.YAMLKeyValue;

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
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("doctrine.orm.yml"));
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

        // @TODO: Fix namespace collision
        //assertContainsElements(map.keySet(), "FooBundle:Doc");
        //assertContainsElements(map.values(), "\\FooBundle\\Document");

        assertContainsElements(map.keySet(), "FooBundle:Couch");
        assertContainsElements(map.values(), "\\FooBundle\\CouchDocument");

        // class fallback
        assertContainsElements(map.keySet(), "FooBundle\\Entity\\Bar");

        // interface; instance blacklist
        assertFalse(map.values().contains("FooBundle:BarRepository"));
        assertFalse(map.values().contains("FooBundle:BarInterface"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper#getModelFieldTargets
     */
    public void testGetModelFieldTargets() {
        PsiElement[] names = EntityHelper.getModelFieldTargets(PhpElementsUtil.getClass(getProject(), "FooBundle\\Entity\\Yaml"), "name");

        assertNotNull(ContainerUtil.find(names, new Condition<PsiElement>() {
            @Override
            public boolean value(PsiElement psiElement) {
                return psiElement instanceof YAMLKeyValue && ((YAMLKeyValue) psiElement).getKeyText().equals("name");
            }
        }));
    }
}
