package fr.adrienbrault.idea.symfony2plugin.tests.util.resource;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil
 */
public class FileResourceUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\HttpKernel\\Bundle{\n" +
                "    interface Bundle {}\n" +
                "}\n" +
                "namespace FooBundle {\n" +
                "    class FooBundle implements \\Symfony\\Component\\HttpKernel\\Bundle\\Bundle {}\n" +
                "}"
        );

        myFixture.configureByText("target.xml", "" +
                "<routes>\n" +
                "    <import resource=\"@FooBundle/foo.xml\" />\n" +
                "</routes>"
        );
    }

    public void testGetFileResourceRefers() {
        PsiFile psiFile = myFixture.configureByText("foo.xml", "foo");

        assertNotNull(ContainerUtil.find(FileResourceUtil.getFileResourceRefers(getProject(), psiFile.getVirtualFile()), new Condition<VirtualFile>() {
            @Override
            public boolean value(VirtualFile virtualFile) {
                return virtualFile.getName().equals("target.xml");
            }
        }));
    }
}
