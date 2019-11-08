package fr.adrienbrault.idea.symfonyplugin.tests.util;

import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PhpTypeProviderUtil;

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
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/util/fixtures";
    }

    /**
     * @see PhpTypeProviderUtil#getResolvedParameter
     */
    public void testGetResolvedParameter() {
        assertEquals("Class\\Foo", PhpTypeProviderUtil.getResolvedParameter(PhpIndex.getInstance(getProject()), "#K#C\\Class\\Foo.class"));
    }

    /**
     * @see PhpTypeProviderUtil#getResolvedParameter
     */
    public void testGetTypeSignature() {
        Function<PhpNamedElement, String> func = phpNamedElement ->
            phpNamedElement instanceof Method ? ((Method) phpNamedElement).getContainingClass().getFQN() : null;

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

        Collection<PhpNamedElement> phpNamedElements = new ArrayList<>();
        phpNamedElements.add(PhpElementsUtil.getClassMethod(getProject(), "PhpType\\Bar", "foo"));
        phpNamedElements.add(PhpElementsUtil.getClassMethod(getProject(), "PhpType\\Bar", "bar"));
        phpNamedElements.add(PhpElementsUtil.getClassMethod(getProject(), "PhpType\\Bar", "car"));

        Collection<? extends PhpNamedElement> elements = PhpTypeProviderUtil.mergeSignatureResults(phpNamedElements, PhpElementsUtil.getClass(getProject(), "\\PhpType\\Foo"));
        assertEquals(2, elements.size());
    }

    /**
     * @see PhpTypeProviderUtil#getReferenceSignatureByFirstParameter
     */
    public void testGetReferenceSignature() {
        assertEquals("#F\\foo|foobar", PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(
            PhpPsiElementFactory.createFunctionReference(getProject(), "<?php foo('foobar');"), "|".charAt(0))
        );

        assertEquals("#F\\foo|#K#C\\Foo.class", PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(
            PhpPsiElementFactory.createFunctionReference(getProject(), "<?php class Foo {}; foo(Foo::class);"), "|".charAt(0))
        );

        assertEquals("#F\\foo|#P#C\\Foo.foo", PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(
            PhpPsiElementFactory.createFunctionReference(getProject(), "<?php class Foo { private $foo = 'foobar' \n private function() { foo($this->foo); } }; "), "|".charAt(0))
        );
    }
}
