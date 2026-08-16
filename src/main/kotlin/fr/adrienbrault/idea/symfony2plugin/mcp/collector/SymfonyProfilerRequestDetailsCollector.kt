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
     * @param hash exact hexadecimal profiler token
     * @param collector optional collector name; `null` renders the compact overview
     * @param page 1-based page for the paginated collector detail view
     */
    fun collect(hash: String, collector: String? = null, page: Int = 1): String {
        val normalizedHash = hash.trim()
        if (!normalizedHash.matches(Regex("^[a-fA-F0-9]{6,64}$"))) {
            mcpFail("hash must be a 6-64 character hexadecimal profiler token.")
        }

        val requestedCollector = collector?.trim()?.takeIf { it.isNotEmpty() }

        val rawProfile = profilerIndex.getRawProfile(normalizedHash)
            ?: mcpFail(
                "Raw profiler data for '$normalizedHash' is not available. " +
                    "Profiler details currently require a local Symfony profiler file.",
            )

        val profile = try {
            SymfonyProfilerProfile.read(rawProfile)
        } catch (exception: Exception) {
            mcpFail("Unable to parse profiler request '$normalizedHash': ${exception.message}")
        }

        val selectedCollector = requestedCollector
        if (selectedCollector != null && selectedCollector !in profile.collectorNames) {
            mcpFail("Profiler request '$normalizedHash' does not contain the '$selectedCollector' collector.")
        }

        return try {
            SymfonyProfilerRequestDetailsRenderer.render(profile, normalizedHash, selectedCollector, page)
        } catch (exception: ProfilerRendererException) {
            mcpFail(
                "Unable to read the '${exception.collectorName}' collector for '$normalizedHash': " +
                    exception.message,
            )
        }
    }
}
