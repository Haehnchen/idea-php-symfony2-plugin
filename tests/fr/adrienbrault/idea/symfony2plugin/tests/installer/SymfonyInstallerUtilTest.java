package fr.adrienbrault.idea.symfony2plugin.tests.installer;

import fr.adrienbrault.idea.symfony2plugin.installer.SymfonyInstallerUtil;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SymfonyInstallerUtilTest extends Assert {

    @Test
    public void testExtractSuccessMessage() {

        String s = SymfonyInstallerUtil.extractSuccessMessage("    4.95 MB/4.95 MB ====================================================\n" +
            "  100%\n" +
            "\n" +
            " Preparing project...\n" +
            "\n" +
            " OK  Symfony 2.7.1 was successfully installed. Now you can:\n" +
            "\n" +
            "    * Change your current directory to D:\\test\\foo\n" +
            "\n" +
            "    * Configure your application in app/config/parameters.yml file. "
        );

        assertTrue(s.endsWith("yml file."));
        assertTrue(s.startsWith("OK"));

        assertNull(SymfonyInstallerUtil.extractSuccessMessage("foo"));
    }

    @Test
    public void testFormatConsoleTextIndicatorOutput() {
        assertEquals("41.2% - 2.03 MB/4.95 MB", SymfonyInstallerUtil.formatConsoleTextIndicatorOutput("2.03 MB/4.95 MB ======>---   41.2%"));
        assertEquals("41% - 2.03 MB/4.95 MB", SymfonyInstallerUtil.formatConsoleTextIndicatorOutput("2.03 MB/4.95 MB ======>---   41%"));
        assertEquals("foo", SymfonyInstallerUtil.formatConsoleTextIndicatorOutput("foo"));
    }

    @Test
    public void formatGetVersions() {

        List<SymfonyInstallerVersion> versions = SymfonyInstallerUtil.getVersions("{\"lts\":\"2.7.1\",\"latest\":\"2.7.1\",\"dev\":\"2.8.0-dev\",\"2.3\":\"2.3.30\",\"2.6\":\"2.6.9\",\"2.7\":\"2.7.1\",\"2.8\":\"2.8.0-dev\",\"non_installable\":[\"2.0.0\",\"2.0.1\",\"2.0.2\",\"2.0.3\",\"2.0.4\",\"2.0.5\",\"2.0.6\",\"2.0.7\",\"2.0.8\",\"2.0.9\",\"2.0.10\",\"2.0.11\",\"2.0.12\",\"2.0.13\",\"2.0.14\",\"2.0.15\",\"2.0.16\",\"2.0.17\",\"2.0.18\",\"2.0.19\",\"2.0.20\",\"2.0.21\",\"2.0.22\",\"2.0.23\",\"2.0.24\",\"2.0.25\",\"2.1.0\",\"2.1.1\",\"2.1.2\",\"2.1.3\",\"2.1.4\",\"2.1.5\",\"2.1.6\",\"2.1.7\",\"2.1.8\",\"2.1.9\",\"2.1.10\",\"2.1.11\",\"2.1.12\",\"2.1.13\",\"2.2.0\",\"2.2.1\",\"2.2.2\",\"2.2.3\",\"2.2.4\",\"2.2.5\",\"2.2.6\",\"2.2.7\",\"2.2.8\",\"2.2.9\",\"2.2.10\",\"2.2.11\",\"2.3.0\",\"2.3.1\",\"2.3.2\",\"2.3.3\",\"2.3.4\",\"2.3.5\",\"2.3.6\",\"2.3.7\",\"2.3.8\",\"2.3.9\",\"2.3.10\",\"2.3.11\",\"2.3.12\",\"2.3.13\",\"2.3.14\",\"2.3.15\",\"2.3.16\",\"2.3.17\",\"2.3.18\",\"2.3.19\",\"2.3.20\",\"2.4.0\",\"2.4.1\",\"2.4.2\",\"2.4.3\",\"2.4.4\",\"2.4.5\",\"2.4.6\",\"2.4.7\",\"2.4.8\",\"2.4.9\",\"2.4.10\"],\"installable\":[\"2.3.21\",\"2.3.22\",\"2.3.23\",\"2.3.24\",\"2.3.25\",\"2.3.26\",\"2.3.27\",\"2.3.28\",\"2.3.29\",\"2.3.30\",\"2.6.0\",\"2.6.1\",\"2.6.2\",\"2.6.3\",\"2.6.4\",\"2.6.5\",\"2.6.6\",\"2.6.7\",\"2.6.8\",\"2.6.9\"]}");

        assertEquals("latest", versions.get(0).getVersion());
        assertEquals("2.7.1 (latest)", versions.get(0).getPresentableName());

        assertEquals("2.6.9", versions.get(3).getVersion());
        assertEquals("2.6.9", versions.get(3).getPresentableName());
    }
}
