package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.ProfilerRendererException
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerRequestDetailsRenderer

/** Loads one profiler request and delegates its presentation to profiler renderers. */
class SymfonyProfilerRequestDetailsCollector(
    private val profilerIndex: ProfilerIndexInterface,
) {
    /**
     * @param hash exact hexadecimal profiler token or `latest` (newest known request)
     * @param collector optional collector name; `null` renders the compact overview
     * @param page 1-based page for the paginated collector detail view
     */
    fun collect(hash: String, collector: String? = null, page: Int = 1): String {
        val normalizedHash = hash.trim()
        val recentRequests by lazy(LazyThreadSafetyMode.NONE) { profilerIndex.requests }
        val resolvedHash = if (normalizedHash == "latest") {
            recentRequests.firstOrNull()?.hash
                ?: mcpFail("No profiler requests are available to resolve 'latest'.")
        } else {
            normalizedHash
        }

        if (!resolvedHash.matches(Regex("^[a-fA-F0-9]{6,64}$"))) {
            mcpFail("hash must be 'latest' or a 6-64 character hexadecimal profiler token.")
        }

        val requestedCollector = collector?.trim()?.takeIf { it.isNotEmpty() }

        val rawProfile = profilerIndex.getRawProfile(resolvedHash)
            ?: mcpFail(
                "Raw profiler data for '$resolvedHash' is not available. " +
                    "Profiler details currently require a local Symfony profiler file.",
            )

        val profile = try {
            SymfonyProfilerProfile.read(rawProfile)
        } catch (exception: Exception) {
            val newestHashes = recentRequests
                .asSequence()
                .map { it.hash }
                .take(10)
                .joinToString(", ")
                .ifEmpty { "(none)" }
            mcpFail(
                "Unable to parse profiler request '$resolvedHash': ${exception.message}. " +
                    "Newest profiler hashes: $newestHashes",
            )
        }

        if (requestedCollector != null && requestedCollector !in profile.collectorNames) {
            val availableCollectors = profile.collectorNames.joinToString(", ").ifEmpty { "(none)" }
            mcpFail(
                "Profiler request '$resolvedHash' does not contain the '$requestedCollector' collector. " +
                    "Available collectors: $availableCollectors",
            )
        }

        return try {
            SymfonyProfilerRequestDetailsRenderer.render(profile, resolvedHash, requestedCollector, page)
        } catch (exception: ProfilerRendererException) {
            mcpFail(
                "Unable to read the '${exception.collectorName}' collector for '$resolvedHash': " +
                    exception.message,
            )
        }
    }
}
