package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile

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
