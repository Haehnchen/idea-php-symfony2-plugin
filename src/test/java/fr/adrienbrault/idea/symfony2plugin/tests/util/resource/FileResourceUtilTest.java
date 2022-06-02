package fr.adrienbrault.idea.symfony2plugin.tests.util.resource;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil
 */
public class FileResourceUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/resource/fixtures";
    }

    public void testGetFileResourceRefers() {
        createBundleScopeProject();

        PsiFile psiFile = myFixture.configureByText("foo.xml", "foo");
        assertNotNull(ContainerUtil.find(FileResourceUtil.getFileResourceRefers(getProject(), psiFile.getVirtualFile()), virtualFile -> virtualFile.getName().equals("target.xml")));
    }

    public void testGetFileResourceTargetsInBundleDirectory() {
        createBundleScopeProject();

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

    public void testFileResources() {
        VirtualFile services = myFixture.copyFileToProject("services.xml", "config/services.xml");
        VirtualFile virtualFile = myFixture.copyFileToProject("classes.php", "src/Test.php");

        assertTrue(FileResourceUtil.hasFileResources(getProject(), PsiElementUtils.virtualFileToPsiFile(getProject(), virtualFile)));

        Collection<Pair<VirtualFile, String>> fileResources = FileResourceUtil.getFileResources(getProject(), virtualFile);
        assertTrue(fileResources.stream().anyMatch(pair -> pair.getFirst().getPath().equals(services.getPath())));
    }

    public void testFileResourcesForBundle() {
        myFixture.copyFileToProject("classes.php");
        myFixture.configureByText("target.xml", "" +
            "<routes>\n" +
            "    <import resource=\"@FooBundle/*Controller.php\" />\n" +
            "</routes>"
        );

        VirtualFile virtualFile = myFixture.copyFileToProject("dummy.php", "src/TestController.php");

        assertTrue(FileResourceUtil.hasFileResources(getProject(), PsiManager.getInstance(getProject()).findFile(virtualFile)));

        Collection<Pair<VirtualFile, String>> fileResources = FileResourceUtil.getFileResources(getProject(), virtualFile);
        assertTrue(fileResources.stream().anyMatch(pair -> pair.getSecond().equals("@FooBundle/*Controller.php")));
    }

    public void testFileResourcesForBundleNoMatch() {
        myFixture.copyFileToProject("classes.php");

        PsiFile psiFile = myFixture.configureByText("target.xml", "" +
            "<routes>\n" +
            "    <import resource=\"@FooBundle/*Foobar.php\" />\n" +
            "</routes>"
        );

        VirtualFile virtualFile = myFixture.copyFileToProject("dummy.php", "src/TestController.php");

        assertFalse(FileResourceUtil.hasFileResources(getProject(), PsiElementUtils.virtualFileToPsiFile(getProject(), virtualFile)));

        Collection<Pair<VirtualFile, String>> fileResources = FileResourceUtil.getFileResources(getProject(), virtualFile);
        assertFalse(fileResources.stream().anyMatch(pair -> pair.getFirst().getPath().equals(psiFile.getVirtualFile().getPath())));
    }

    public void testGetFileImplementsLineMarker() {
        myFixture.copyFileToProject("services.xml", "config/services.xml");
        VirtualFile virtualFile = myFixture.copyFileToProject("classes.php", "src/Test.php");
        assertNotNull(FileResourceUtil.getFileImplementsLineMarker(PsiElementUtils.virtualFileToPsiFile(getProject(), virtualFile)));
    }

    public void testGetFileImplementsLineMarkerForGlob() {
        myFixture.copyFileToProject("services.xml", "config/services.xml");
        VirtualFile virtualFile = myFixture.copyFileToProject("classes.php", "src/TestController.php");
        assertNotNull(FileResourceUtil.getFileImplementsLineMarker(PsiElementUtils.virtualFileToPsiFile(getProject(), virtualFile)));
    }

    public void testGetFileImplementsLineMarkerForBundle() {
        createBundleScopeProject();

        myFixture.copyFileToProject("services.xml", "config/services.xml");
        VirtualFile virtualFile = myFixture.copyFileToProject("classes.php", "src/Test.php");
        assertNotNull(FileResourceUtil.getFileImplementsLineMarker(PsiElementUtils.virtualFileToPsiFile(getProject(), virtualFile)));
    }

    private void createBundleScopeProject() {
        myFixture.copyFileToProject("classes.php");
        myFixture.configureByText("target.xml", "" +
            "<routes>\n" +
            "    <import resource=\"@FooBundle/foo.xml\" />\n" +
            "</routes>"
        );
    }
}
