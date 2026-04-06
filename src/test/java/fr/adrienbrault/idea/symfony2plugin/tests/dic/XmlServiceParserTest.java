package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;

public class XmlServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testParserMergesMultipleCompiledContainers() throws Exception {
        VirtualFile installerContainer = myFixture.addFileToProject(
            "var/cache/dev/Project_Core_Installer_InstallerKernelDevDebugContainer.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><container><service id=\"installer.service\" class=\"App\\\\Installer\\\\InstallerService\"/></container>"
        ).getVirtualFile();

        VirtualFile kernelContainer = myFixture.addFileToProject(
            "var/cache/dev_hf90b29260a1c6356fa4264a02dec50e9/Project_Core_KernelDevDebugContainer.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><container><service id=\"kernel.service\" class=\"App\\\\Kernel\\\\KernelService\"/></container>"
        ).getVirtualFile();

        XmlServiceParser parser = new XmlServiceParser();

        try (InputStream inputStream = installerContainer.getInputStream()) {
            parser.parser(inputStream, installerContainer, getProject());
        }

        try (InputStream inputStream = kernelContainer.getInputStream()) {
            parser.parser(inputStream, kernelContainer, getProject());
        }

        assertTrue(parser.getServiceMap().getIds().contains("installer.service"));
        assertTrue(parser.getServiceMap().getIds().contains("kernel.service"));
        assertEquals(2, parser.getServiceMap().getServices().size());
    }
}
