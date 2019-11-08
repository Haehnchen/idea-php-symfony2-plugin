package fr.adrienbrault.idea.symfonyplugin.tests.dic.tags;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.dic.tags.ServiceTagFactory;
import fr.adrienbrault.idea.symfonyplugin.dic.tags.ServiceTagInterface;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.dic.tags.ServiceTagFactory
 */
public class ServiceTagFactoryTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testServiceTagFactoryCreationForYaml() {
        myFixture.configureByText(YAMLFileType.YML, "" +
            "services:\n" +
            "   fo<caret>o:\n" +
            "       tags:\n" +
            "        - { name: 'foobar_tag', method: 'car' }"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement parent = psiElement.getParent();

        Collection<ServiceTagInterface> foobar = ServiceTagFactory.create("foobar", parent);
        ServiceTagInterface serviceTag = foobar.iterator().next();

        assertEquals("foo", serviceTag.getServiceId());
        assertEquals("foobar_tag", serviceTag.getAttribute("name"));
        assertEquals("foobar_tag", serviceTag.getName());
    }
}
