package fr.adrienbrault.idea.symfony2plugin.tests.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.collector.LocalTwigComponentDataCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerTwigComponent;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LocalTwigComponentDataCollectorTest extends TestCase {
    public void testGetComponentsFromVarDumperDataPayload() {
        LocalTwigComponentDataCollector collector = new LocalTwigComponentDataCollector("""
            a:2:{s:14:"twig_component";O:65:"Symfony\\UX\\TwigComponent\\DataCollector\\TwigComponentDataCollector":1:{s:4:"data";O:39:"Symfony\\Component\\VarDumper\\Cloner\\Data":7:{s:45:"\0Symfony\\Component\\VarDumper\\Cloner\\Data\0data";a:7:{i:0;a:1:{i:0;a:1:{i:1;i:1;}}i:1;a:6:{s:10:"components";a:1:{i:1;i:2;}s:15:"component_count";i:3;s:7:"renders";a:1:{i:1;i:3;}s:12:"render_count";i:3;s:11:"render_time";d:1.80;s:17:"peak_memory_usage";i:1000;}i:2;a:3:{s:11:"AlertBanner";a:1:{i:1;i:4;}s:9:"Shop:Card";a:1:{i:1;i:5;}s:11:"TabSwitcher";a:1:{i:1;i:6;}}i:3;a:0:{}i:4;a:7:{s:4:"name";s:11:"AlertBanner";s:5:"class";s:43:"Symfony\\UX\\TwigComponent\\AnonymousComponent";s:10:"class_stub";O:39:"Symfony\\Component\\VarDumper\\Cloner\\Stub":2:{s:5:"value";s:43:"Symfony\\UX\\TwigComponent\\AnonymousComponent";s:4:"attr";a:2:{s:4:"file";s:34:"/app/vendor/AnonymousComponent.php";s:4:"line";i:21;}}s:8:"template";s:32:"components/AlertBanner.html.twig";s:13:"template_path";s:42:"/app/templates/components/AlertBanner.html.twig";s:12:"render_count";i:2;s:11:"render_time";d:0.62;}i:5;a:6:{s:4:"name";s:9:"Shop:Card";s:5:"class";s:28:"App\\Twig\\Components\\ShopCard";s:8:"template";s:30:"components/Shop/Card.html.twig";s:13:"template_path";N;s:12:"render_count";i:5;s:11:"render_time";d:0.23;}i:6;a:6:{s:4:"name";s:11:"TabSwitcher";s:5:"class";s:43:"Symfony\\UX\\TwigComponent\\AnonymousComponent";s:8:"template";s:32:"components/TabSwitcher.html.twig";s:13:"template_path";N;s:12:"render_count";i:1;s:11:"render_time";d:0.95;}}}}s:4:"next";a:0:{}}
            """);

        List<ProfilerTwigComponent> components = new ArrayList<>(collector.getComponents());

        assertEquals(3, components.size());

        ProfilerTwigComponent alertBanner = components.get(0);
        assertEquals("AlertBanner", alertBanner.name());
        assertTrue(alertBanner.isAnonymous());
        assertEquals("components/AlertBanner.html.twig", alertBanner.template());
        assertEquals(2, alertBanner.renderCount());

        ProfilerTwigComponent shopCard = components.get(1);
        assertEquals("Shop:Card", shopCard.name());
        assertEquals("\\App\\Twig\\Components\\ShopCard", shopCard.className());
        assertEquals("components/Shop/Card.html.twig", shopCard.template());
        assertEquals(5, shopCard.renderCount());

        ProfilerTwigComponent tabSwitcher = components.get(2);
        assertEquals("TabSwitcher", tabSwitcher.name());
        assertTrue(tabSwitcher.isAnonymous());
        assertEquals(1, tabSwitcher.renderCount());
    }

    public void testGetComponentsWithoutTwigComponentCollector() {
        Collection<ProfilerTwigComponent> components = new LocalTwigComponentDataCollector("a:0:{}").getComponents();

        assertTrue(components.isEmpty());
    }

    public void testProfilerTwigComponentRequiresFullyQualifiedClassName() {
        try {
            new ProfilerTwigComponent("Shop:Card", "App\\Twig\\Components\\ShopCard", "components/Shop/Card.html.twig", 1);
            fail("Expected className without leading backslash to fail");
        } catch (IllegalArgumentException ignored) {
        }
    }
}
