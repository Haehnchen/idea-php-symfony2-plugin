package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile

internal const val PROFILER_DETAIL_PAGE_TOKEN_TARGET = 4_000
internal const val APPROXIMATE_CHARACTERS_PER_TOKEN = 3.75
internal val PROFILER_DETAIL_PAGE_CHARACTER_TARGET =
    (PROFILER_DETAIL_PAGE_TOKEN_TARGET * APPROXIMATE_CHARACTERS_PER_TOKEN).toInt()

/** Provides compact and collector-specific detail views for one profiler collector. */
internal interface ProfilerDetailRenderer {
    /** Collector name used by the MCP selector and availability check. */
    val name: String

    /** Higher weights place the collector earlier in the request overview. */
    val overviewWeight: Int
        get() = 0

    /** Renders optional content embedded in the compact request overview. */
    fun renderOverview(profile: SymfonyProfilerProfile): String? = null

    /** Renders the collector-specific detail view. */
    fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String
}

/** One detail page whose entries are always kept intact. */
internal data class ProfilerDetailPage(
    val entries: List<String>,
    val number: Int,
    val total: Int,
) {
    val isPaginated: Boolean
        get() = total > 1
}

/**
 * Paginates complete rendered entries around the approximate token target.
 *
 * The entry crossing the character target remains on the current page. This makes the target soft
 * and avoids splitting a rendered value or database query between pages.
 */
internal fun paginateProfilerDetailEntries(
    entries: List<String>,
    requestedPage: Int,
    characterTarget: Int = PROFILER_DETAIL_PAGE_CHARACTER_TARGET,
): ProfilerDetailPage {
    require(characterTarget > 0) { "characterTarget must be greater than zero" }

    val pages = mutableListOf<List<String>>()
    var currentEntries = mutableListOf<String>()
    var currentCharacterCount = 0

    entries.forEach { entry ->
        currentEntries.add(entry)
        currentCharacterCount += entry.length + 1

        if (currentCharacterCount >= characterTarget) {
            pages.add(currentEntries)
            currentEntries = mutableListOf()
            currentCharacterCount = 0
        }
    }

    if (currentEntries.isNotEmpty() || pages.isEmpty()) {
        pages.add(currentEntries)
    }

    val currentPage = requestedPage.coerceIn(1, pages.size)
    return ProfilerDetailPage(pages[currentPage - 1], currentPage, pages.size)
}
