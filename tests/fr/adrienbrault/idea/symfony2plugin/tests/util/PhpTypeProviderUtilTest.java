package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTypeProviderUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpTypeProviderUtil.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see PhpTypeProviderUtil#getResolvedParameter
     */
    public void testGetResolvedParameter() {
        assertEquals("\\Class\\Foo", PhpTypeProviderUtil.getResolvedParameter(PhpIndex.getInstance(getProject()), "#K#C\\Class\\Foo."));
        assertEquals("\\Class\\Foo", PhpTypeProviderUtil.getResolvedParameter(PhpIndex.getInstance(getProject()), "#K#C\\Class\\Foo.class"));
    }

    /**
     * @see PhpTypeProviderUtil#getResolvedParameter
     */
    public void testGetTypeSignature() {
        Function<PhpNamedElement, String> func = new Function<PhpNamedElement, String>() {
            @Override
            public String fun(PhpNamedElement phpNamedElement) {
                return phpNamedElement instanceof Method ? ((Method) phpNamedElement).getContainingClass().getFQN() : null;
            }
        };

        ArrayList<? extends PhpNamedElement> typeSignature = new ArrayList<PhpNamedElement>(PhpTypeProviderUtil.getTypeSignature(
            PhpIndex.getInstance(getProject()),
            "#M#C\\Doctrine\\Common\\Persistence\\ObjectManager.getRepository|#M#C\\Doctrine\\Common\\Persistence\\ObjectFoo.getRepository"
        ));

        assertContainsElements(ContainerUtil.map(typeSignature, func), "\\Doctrine\\Common\\Persistence\\ObjectManager", "\\Doctrine\\Common\\Persistence\\ObjectFoo");

        typeSignature = new ArrayList<PhpNamedElement>(PhpTypeProviderUtil.getTypeSignature(
            PhpIndex.getInstance(getProject()),
            "#M#C\\Doctrine\\Common\\Persistence\\ObjectManager.getRepository"
        ));
        assertContainsElements(ContainerUtil.map(typeSignature, func), "\\Doctrine\\Common\\Persistence\\ObjectManager");
    }

    /**
     * @see PhpTypeProviderUtil#mergeSignatureResults
     */
    public void testMergeSignatureResults() {

        Collection<PhpNamedElement> phpNamedElements = new ArrayList<PhpNamedElement>();
        phpNamedElements.add(PhpElementsUtil.getClassMethod(getProject(), "PhpType\\Bar", "foo"));
        phpNamedElements.add(PhpElementsUtil.getClassMethod(getProject(), "PhpType\\Bar", "bar"));
        phpNamedElements.add(PhpElementsUtil.getClassMethod(getProject(), "PhpType\\Bar", "car"));

        Collection<? extends PhpNamedElement> elements = PhpTypeProviderUtil.mergeSignatureResults(phpNamedElements, PhpElementsUtil.getClass(getProject(), "\\PhpType\\Foo"));
        assertEquals(2, elements.size());
    }
}
