package fr.adrienbrault.idea.symfony2plugin.tests.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.collector.LocalDefaultDataCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class LocalDefaultDataCollectorTest extends TestCase {
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/profiler/collector/fixtures";
    }

    public void testGetTemplate() {
        String data = ProfilerUtil.getContentForFile(new File(this.getTestDataPath() + "/template-d6bc80"));

        LocalDefaultDataCollector localDefaultDataCollector = new LocalDefaultDataCollector(data);
        String template = localDefaultDataCollector.getTemplate();

        assertEquals("test.html.twig", template);
    }

    public void testGetRenderedTemplates() {
        String data = ProfilerUtil.getContentForFile(new File(this.getTestDataPath() + "/template-d6bc80"));

        LocalDefaultDataCollector localDefaultDataCollector = new LocalDefaultDataCollector(data);
        ArrayList<String> templates = new ArrayList<>(localDefaultDataCollector.getRenderedTemplates());

        assertEquals(Arrays.asList(
            "test.html.twig",
            "base.html.twig"
        ), templates.subList(0, 2));
        assertTrue(templates.contains("@WebProfiler/Profiler/toolbar_js.html.twig"));
    }

    public void testGetFormTypes() {
        String data = ProfilerUtil.getContentForFile(new File(this.getTestDataPath() + "/form-e72b50"));

        LocalDefaultDataCollector localDefaultDataCollector = new LocalDefaultDataCollector(data);
        @NotNull Collection<String> template = localDefaultDataCollector.getFormTypes();

        assertSame(1, template.size());
        assertEquals("App\\Form\\MyFormType", template.iterator().next());
    }
}
