package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SerializerUtil;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SerializerUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }

    public void testVisitSerializerMethodReferencer() {
        PsiFile psiFile = myFixture.configureByFile("SerializerStubIndex.php");

        Collection<String> users = new HashSet<>();
        SerializerUtil.visitSerializerMethodReference(psiFile, pair -> users.add(pair.getFirst()));
        assertContainsElements(users, "\\app\\foobar2");
    }

    public void testGetClassTargetForSerializer() {
        PsiFile psiFile = myFixture.configureByFile("SerializerStubIndex.php");
        assertTrue((long) SerializerUtil.getClassTargetForSerializer(psiFile.getProject(), "\\App\\Foobar2").size() > 0);
    }

    public void testHasClassTargetForSerializer() {
        PsiFile psiFile = myFixture.configureByFile("SerializerStubIndex.php");
        assertTrue(SerializerUtil.hasClassTargetForSerializer(psiFile.getProject(), "\\App\\Foobar2"));
    }
}
