package fr.adrienbrault.idea.symfony2plugin.tests.util.resource;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil
 */
public class FileResourceUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("classes.php");
        myFixture.configureByText("target.xml", "" +
                "<routes>\n" +
                "    <import resource=\"@FooBundle/foo.xml\" />\n" +
                "</routes>"
        );
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/resource/fixtures";
    }

    public void testGetFileResourceRefers() {
        PsiFile psiFile = myFixture.configureByText("foo.xml", "foo");

        assertNotNull(ContainerUtil.find(FileResourceUtil.getFileResourceRefers(getProject(), psiFile.getVirtualFile()), virtualFile -> virtualFile.getName().equals("target.xml")));
    }

    public void testGetFileResourceTargetsInBundleDirectory() {
        for (String s : new String[]{"@FooBundle/Controller", "@FooBundle\\Controller", "@FooBundle/Controller/", "@FooBundle//Controller", "@FooBundle\\Controller\\"}) {
            assertNotNull(ContainerUtil.find(FileResourceUtil.getFileResourceTargetsInBundleDirectory(getProject(), s), psiElement ->
                psiElement instanceof PhpClass && "\\FooBundle\\Controller\\FooController".equals(((PhpClass) psiElement).getFQN())
            ));
        }
    }

    public void testGetGlobalPatternDirectory() {
        @NotNull Pair<String, String> globalPatternDirectory = FileResourceUtil.getGlobalPatternDirectory("../src/DependencyInjection/");
        assertEquals("../src/DependencyInjection/", globalPatternDirectory.getFirst());
        assertNull(globalPatternDirectory.getSecond());

        globalPatternDirectory = FileResourceUtil.getGlobalPatternDirectory("../src/DependencyInjection/**/test.php");
        assertEquals("../src/DependencyInjection", globalPatternDirectory.getFirst());
        assertEquals("**/test.php", globalPatternDirectory.getSecond());

        globalPatternDirectory = FileResourceUtil.getGlobalPatternDirectory("../src/{Entity,Foobar}/");
        assertEquals("../src", globalPatternDirectory.getFirst());
        assertEquals("{Entity,Foobar}/", globalPatternDirectory.getSecond());

        globalPatternDirectory = FileResourceUtil.getGlobalPatternDirectory("../src/Kernel.php");
        assertEquals("../src/Kernel.php", globalPatternDirectory.getFirst());
        assertNull(globalPatternDirectory.getSecond());

        globalPatternDirectory = FileResourceUtil.getGlobalPatternDirectory("src/");
        assertEquals("src/", globalPatternDirectory.getFirst());
        assertNull(globalPatternDirectory.getSecond());
    }
}
