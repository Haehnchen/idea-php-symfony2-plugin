package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslationConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslationState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SymfonyProfilerTranslationConsumerTest {
    @Test
    fun `reads translation summary and messages from VarDumper data`() {
        val actual = SymfonyProfilerTranslationConsumer.read(
            SymfonyProfilerProfile.read(resourceFixture()),
        )

        assertEquals("en", actual.locale)
        assertEquals(listOf("fr", "de"), actual.fallbackLocales)
        assertEquals(2, actual.definedCount)
        assertEquals(2, actual.missingCount)
        assertEquals(1, actual.fallbackCount)
        assertEquals(5, actual.messages.size)

        val missing = actual.messages.first { it.id == "checkout.missing_title" }
        assertEquals(SymfonyProfilerTranslationState.MISSING, missing.state)
        assertEquals("checkout", missing.domain)
        assertEquals(2, missing.count)

        val fallback = actual.messages.first { it.id == "account.title" }
        assertEquals(SymfonyProfilerTranslationState.FALLBACK, fallback.state)
        assertEquals("fr", fallback.fallbackLocale)
        assertEquals("Compte", fallback.translation)

        // Runtime parameters are deliberately not retained by the specialized model.
        val renderedModel = "$actual"
        assertFalse("%name%" in renderedModel)
        assertFalse("Example" in renderedModel)
    }

    private fun resourceFixture(): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/symfony-profiler-translation.gz"),
    ).use { it.readAllBytes() }
}
