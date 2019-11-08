package fr.adrienbrault.idea.symfonyplugin.tests.external.toolbox.provider;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.type.PhpToolboxTypeProviderArguments;
import fr.adrienbrault.idea.symfonyplugin.external.toolbox.provider.ServiceToolboxProvider;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.Collection;

public class ServiceToolboxProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/external/toolbox/provider/fixtures";
    }

    /**
     * @see ServiceToolboxProvider#resolveParameter
     */
    public void testTypeResolve() {
        ServiceToolboxProvider provider = new ServiceToolboxProvider();
        Collection<PhpNamedElement> classes = provider.resolveParameter(
            new PhpToolboxTypeProviderArguments(getProject(), "foo_bar_foo_Bar", new ArrayList<>())
        );

        assertNotNull(classes);
        assertNotNull(ContainerUtil.find(classes, phpNamedElement ->
            phpNamedElement instanceof PhpClass && "FooBar".equals(phpNamedElement.getName()))
        );
    }

    /**
     * @see ServiceToolboxProvider#resolveParameter
     */
    public void testTypeNullResolve() {
        assertNull(new ServiceToolboxProvider().resolveParameter(
            new PhpToolboxTypeProviderArguments(getProject(), "unknown_xxx", new ArrayList<>())
        ));
    }

    /**
     * @see ServiceToolboxProvider#getPsiTargets
     */
    public void testTargetIsReturnedForService() {
        Collection<PsiElement> classes = new ServiceToolboxProvider().getPsiTargets(
            new PhpToolboxDeclarationHandlerParameter(PhpPsiElementFactory.createComma(getProject()), "foo_bar_foo_Bar", PhpFileType.INSTANCE)
        );

        assertNotNull(classes);
        assertNotNull(ContainerUtil.find(classes, phpNamedElement ->
            phpNamedElement instanceof PhpClass && "FooBar".equals(((PhpClass) phpNamedElement).getName()))
        );
    }
}
