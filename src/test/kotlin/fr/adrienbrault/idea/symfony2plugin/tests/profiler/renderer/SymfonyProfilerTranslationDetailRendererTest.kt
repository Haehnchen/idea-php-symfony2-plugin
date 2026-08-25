package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslation
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslationMessage
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslationState
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerTranslationDetailRenderer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymfonyProfilerTranslationDetailRendererTest {
    private val renderer = SymfonyProfilerTranslationDetailRenderer

    @Test
    fun `overview contains Symfony translation metrics without message data`() {
        val text = renderer.formatOverview(translation())

        assertTrue("## Collector: translation" in text)
        assertTrue("- Default locale: en" in text)
        assertTrue("- Fallback locales: fr, de" in text)
        assertTrue("- Defined messages: 1" in text)
        assertTrue("- Missing messages: 1" in text)
        assertTrue("- Fallback messages: 1" in text)
        assertFalse("state,locale" in text)
        assertFalse("missing.title" in text)
    }

    @Test
    fun `details render actionable messages first as escaped CSV`() {
        val text = renderer.formatDetails(translation())

        assertTrue("### Messages (CSV)" in text)
        assertTrue("state,locale,fallback_locale,domain,count,id,translation" in text)
        assertTrue("missing,en,,messages,2,missing.title,missing.title" in text)
        assertTrue("fallback,en,fr,messages,1,account.title,Compte" in text)
        assertTrue("defined,en,,messages,3,welcome.title,Welcome" in text)
        assertTrue(text.indexOf("missing,en") < text.indexOf("fallback,en"))
        assertTrue(text.indexOf("fallback,en") < text.indexOf("defined,en"))

        val escaped = renderer.formatDetails(
            translation().copy(
                messages = listOf(
                    message(
                        id = "key, \"quoted\"\nnext",
                        translation = "Preview, \"quoted\"\r\nline",
                    ),
                ),
            ),
        )
        assertTrue("\"key, \"\"quoted\"\" next\"" in escaped)
        assertTrue("\"Preview, \"\"quoted\"\" line\"" in escaped)
        assertFalse("quoted\"\nnext" in escaped)
    }

    @Test
    fun `details paginate complete bounded CSV rows`() {
        val messages = (1..20).map { index ->
            message(
                id = "message.$index",
                translation = "preview-$index-${"x".repeat(2_000)}",
            )
        }
        val value = translation().copy(messages = messages)

        val firstPage = renderer.formatDetails(value)
        val secondPage = renderer.formatDetails(value, page = 2)

        assertTrue("### Messages (CSV, page 1 of 2)" in firstPage)
        assertTrue("message.1" in firstPage)
        assertFalse("message.20" in firstPage)
        assertTrue("### Messages (CSV, page 2 of 2)" in secondPage)
        assertTrue("message.20" in secondPage)
        assertFalse("message.1," in secondPage)
    }

    private fun translation() = SymfonyProfilerTranslation(
        locale = "en",
        fallbackLocales = listOf("fr", "de"),
        definedCount = 1,
        missingCount = 1,
        fallbackCount = 1,
        messages = listOf(
            message("welcome.title", "Welcome", SymfonyProfilerTranslationState.DEFINED, count = 3),
            message("missing.title", "missing.title", SymfonyProfilerTranslationState.MISSING, count = 2),
            message(
                "account.title",
                "Compte",
                SymfonyProfilerTranslationState.FALLBACK,
                fallbackLocale = "fr",
            ),
        ),
    )

    private fun message(
        id: String,
        translation: String,
        state: SymfonyProfilerTranslationState = SymfonyProfilerTranslationState.DEFINED,
        count: Long = 1,
        fallbackLocale: String? = null,
    ) = SymfonyProfilerTranslationMessage(
        locale = "en",
        fallbackLocale = fallbackLocale,
        domain = "messages",
        id = id,
        translation = translation,
        state = state,
        count = count,
    )
}
