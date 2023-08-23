package fr.adrienbrault.idea.symfony2plugin.tests.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.collector.LocalDefaultDataCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.LocalMailCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import junit.framework.TestCase;

import java.io.File;

public class LocalDefaultDataCollectorTest extends TestCase {
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/profiler/collector/fixtures";
    }

    public void testFoo() {
        String data = ProfilerUtil.getContentForFile(new File(this.getTestDataPath() + "/template-d6bc80"));

        LocalDefaultDataCollector localDefaultDataCollector = new LocalDefaultDataCollector(data);
        String template = localDefaultDataCollector.getTemplate();

        assertEquals("test.html.twig", template);
    }
}
