package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.APPROXIMATE_CHARACTERS_PER_TOKEN
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.PROFILER_DETAIL_PAGE_CHARACTER_TARGET
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.PROFILER_DETAIL_PAGE_TOKEN_TARGET
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.paginateProfilerDetailEntries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfilerDetailPaginatorTest {
    @Test
    fun `uses four thousand tokens at three and three quarters characters each`() {
        assertEquals(4_000, PROFILER_DETAIL_PAGE_TOKEN_TARGET)
        assertEquals(3.75, APPROXIMATE_CHARACTERS_PER_TOKEN)
        assertEquals(15_000, PROFILER_DETAIL_PAGE_CHARACTER_TARGET)
    }

    @Test
    fun `keeps the entry crossing the target on the current page`() {
        val entries = listOf("12345", "67890", "next")

        val firstPage = paginateProfilerDetailEntries(entries, requestedPage = 1, characterTarget = 10)
        assertEquals(2, firstPage.total)
        assertEquals(listOf("12345", "67890"), firstPage.entries)

        val secondPage = paginateProfilerDetailEntries(entries, requestedPage = 2, characterTarget = 10)
        assertEquals(listOf("next"), secondPage.entries)
    }
}
