package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerTwigDetailRenderer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymfonyProfilerTwigDetailRendererTest {
    private val renderer = SymfonyProfilerTwigDetailRenderer

    @Test
    fun `overview shows first five unique templates and aggregates duplicates`() {
        val text = renderer.renderOverview(readProfile())

        assertTrue("- Render time: 14.00 ms" in text)
        assertTrue("- Template calls: 8" in text)
        assertTrue("- Block calls: 1" in text)
        assertTrue("- Macro calls: 1" in text)
        assertTrue("- Unique templates: 7" in text)
        assertTrue("### First 5 rendered templates (CSV)" in text)
        assertTrue("template,path,render_count" in text)
        assertTrue("components/card.html.twig,templates/components/card.html.twig,2" in text)
        assertTrue("emails/banner.html.twig,templates/emails/banner.html.twig,1" in text)
        assertFalse("@WebProfiler/Profiler/toolbar_js.html.twig" in text)
        assertFalse("### Rendering call tree" in text)
    }

    @Test
    fun `details show all unique templates and complete call tree`() {
        val text = renderer.renderDetails(readProfile(), 99)

        assertTrue("### Rendered templates (CSV)" in text)
        assertTrue("@WebProfiler/Profiler/toolbar_js.html.twig" in text)
        assertTrue("@WebProfiler/Profiler/toolbar.html.twig" in text)
        assertTrue("### Rendering call tree" in text)
        assertTrue("    main 14.00ms/100%" in text)
        assertTrue("    └ catalog/detail.html.twig 12.00ms/86%" in text)
        assertTrue("    │ └ components/price.html.twig::macro(format_price)" in text)
        assertTrue("    │ └ base.html.twig 6.00ms/43%" in text)
        assertTrue("    │   └ catalog/detail.html.twig::block(title)" in text)
        assertTrue("    └ @WebProfiler/Profiler/toolbar_js.html.twig 2.00ms/14%" in text)
        val callTree = text.substringAfter("### Rendering call tree")
        assertEquals(
            2,
            callTree.windowed("components/card.html.twig".length).count { it == "components/card.html.twig" },
        )
        assertFalse("Page:" in text)
    }

    private fun readProfile() = SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-twig.gz"))

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
