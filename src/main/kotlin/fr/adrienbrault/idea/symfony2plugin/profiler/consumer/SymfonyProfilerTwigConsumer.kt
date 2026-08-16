package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpCustomObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpString
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeLimits
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializer
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.utf8StringOrNull
import java.util.Collections
import java.util.IdentityHashMap

/** Reads Twig render metrics, unique templates, and the complete rendering profile tree. */
object SymfonyProfilerTwigConsumer {
    private val TWIG_COLLECTORS = setOf(
        "Symfony\\Bridge\\Twig\\DataCollector\\TwigDataCollector",
        "Symfony\\Bundle\\TwigBundle\\DataCollector\\TwigDataCollector",
    )
    private const val PROFILE_TYPE_TEMPLATE = "template"
    private const val PROFILE_TYPE_BLOCK = "block"
    private const val PROFILE_TYPE_MACRO = "macro"
    private const val PROFILE_TYPE_ROOT = "ROOT"

    fun read(profile: SymfonyProfilerProfile): SymfonyProfilerTwig {
        val collector = profile.collector("twig")
            ?: error("Symfony profile does not contain the twig collector")
        check(collector.className.utf8StringOrNull() in TWIG_COLLECTORS) {
            "Symfony twig collector has an unsupported class"
        }

        val data = collector["data"].resolve(profile.result) as? PhpArray
            ?: error("Symfony twig collector does not contain its data array")
        val templatePaths = readTemplatePaths(data["template_paths"], profile.result)
        val root = ProfileDecoder().decode(data["profile"], profile.result)
        check(root.type == PROFILE_TYPE_ROOT) { "Symfony twig profile does not have a root node" }
        val profiles = root.depthFirst().drop(1).toList()
        val renderedTemplates = LinkedHashMap<String, SymfonyProfilerTwigTemplate>()

        profiles.asSequence()
            .filter { it.type == PROFILE_TYPE_TEMPLATE }
            .forEach { rendered ->
                val existing = renderedTemplates[rendered.template]
                renderedTemplates[rendered.template] = SymfonyProfilerTwigTemplate(
                    name = rendered.template,
                    path = templatePaths[rendered.template],
                    renderCount = (existing?.renderCount ?: 0) + 1,
                )
            }

        return SymfonyProfilerTwig(
            renderTimeMs = root.durationMs,
            templateCallCount = profiles.count { it.type == PROFILE_TYPE_TEMPLATE },
            blockCallCount = profiles.count { it.type == PROFILE_TYPE_BLOCK },
            macroCallCount = profiles.count { it.type == PROFILE_TYPE_MACRO },
            renderedTemplates = renderedTemplates.values.toList(),
            root = root,
        )
    }

    private fun readTemplatePaths(value: PhpValue?, result: PhpUnserializeResult): Map<String, String> {
        val paths = value.resolve(result) as? PhpArray ?: return emptyMap()
        return buildMap {
            paths.entries.forEach { entry ->
                val template = (entry.key as? PhpStringKey)?.bytes?.utf8StringOrNull() ?: return@forEach
                val path = entry.value.resolve(result)?.utf8StringOrNull() ?: return@forEach
                put(template, path)
            }
        }
    }

    private fun SymfonyProfilerTwigProfile.depthFirst(): Sequence<SymfonyProfilerTwigProfile> = sequence {
        yield(this@depthFirst)
        children.forEach { child -> yieldAll(child.depthFirst()) }
    }

    private class ProfileDecoder {
        private val limits = PhpUnserializeLimits()
        private val activeProfiles = Collections.newSetFromMap(IdentityHashMap<PhpValue, Boolean>())
        private var profileCount = 0
        private var nestedPayloadBytes = 0

        fun decode(value: PhpValue?, result: PhpUnserializeResult): SymfonyProfilerTwigProfile {
            val resolved = value.resolve(result)
                ?: error("Symfony twig collector does not contain its serialized profile")
            return when (resolved) {
                is PhpString -> {
                    val nestedResult = PhpUnserializer.unserialize(resolved.bytes.toByteArray(), limits)
                    decodeProfile(nestedResult.root, nestedResult, 0)
                }

                else -> decodeProfile(resolved, result, 0)
            }
        }

        private fun decodeProfile(
            value: PhpValue,
            result: PhpUnserializeResult,
            depth: Int,
        ): SymfonyProfilerTwigProfile {
            check(depth <= limits.maxNestingDepth) { "Symfony twig profile nesting is too deep" }
            check(++profileCount <= limits.maxContainerEntries) { "Symfony twig profile contains too many nodes" }

            val resolved = value.resolve(result) ?: error("Symfony twig profile node is missing")
            check(activeProfiles.add(resolved)) { "Symfony twig profile contains a reference cycle" }
            return try {
                when (resolved) {
                    is PhpObject -> decodeObject(resolved, result, depth)
                    is PhpCustomObject -> decodeCustomObject(resolved, depth)
                    else -> error("Symfony twig profile node has an unsupported value")
                }
            } finally {
                activeProfiles.remove(resolved)
            }
        }

        private fun decodeObject(
            profile: PhpObject,
            result: PhpUnserializeResult,
            depth: Int,
        ): SymfonyProfilerTwigProfile {
            check(profile.className.utf8StringOrNull() in TWIG_PROFILE_CLASSES) {
                "Symfony twig profile has an unsupported class"
            }
            return decodeFields(
                field = { index, name -> profile[index.toLong()] ?: profile[name] },
                result = result,
                depth = depth,
            )
        }

        private fun decodeCustomObject(
            profile: PhpCustomObject,
            depth: Int,
        ): SymfonyProfilerTwigProfile {
            check(profile.className.utf8StringOrNull() in TWIG_PROFILE_CLASSES) {
                "Symfony twig profile has an unsupported class"
            }
            nestedPayloadBytes += profile.payload.size
            check(nestedPayloadBytes <= limits.maxInputBytes) {
                "Symfony twig profile nested payload exceeds the parse limit"
            }

            val payloadResult = PhpUnserializer.unserialize(profile.payload.toByteArray(), limits)
            val fields = payloadResult.root as? PhpArray
                ?: error("Symfony twig legacy profile payload is not an array")
            return decodeFields(
                field = { index, _ -> fields[index.toLong()] },
                result = payloadResult,
                depth = depth,
            )
        }

        private fun decodeFields(
            field: (Int, String) -> PhpValue?,
            result: PhpUnserializeResult,
            depth: Int,
        ): SymfonyProfilerTwigProfile {
            val template = field(0, "template").resolve(result).requireText("template")
            val name = field(1, "name").resolve(result).requireText("name")
            val type = field(2, "type").resolve(result).requireText("type")
            val starts = field(3, "starts").resolve(result) as? PhpArray
            val ends = field(4, "ends").resolve(result) as? PhpArray
            val childrenData = field(5, "profiles").resolve(result) as? PhpArray
            val children = childrenData?.entries.orEmpty().map { child ->
                decodeProfile(child.value, result, depth + 1)
            }
            val measuredDurationMs = ((ends?.number("wt", result) ?: 0.0) -
                (starts?.number("wt", result) ?: 0.0)) * 1000.0
            val durationMs = if (type == PROFILE_TYPE_ROOT && children.isNotEmpty()) {
                children.sumOf { it.durationMs }
            } else {
                measuredDurationMs.coerceAtLeast(0.0)
            }
            check(durationMs.isFinite()) { "Symfony twig profile contains a non-finite duration" }

            return SymfonyProfilerTwigProfile(
                template = template,
                type = type,
                name = name,
                durationMs = durationMs,
                children = children,
            )
        }

        private fun PhpArray.number(key: String, result: PhpUnserializeResult): Double? =
            when (val value = this[key].resolve(result)) {
                is PhpFloat -> value.value
                is PhpInteger -> value.value.toDouble()
                else -> null
            }

        private fun PhpValue?.requireText(field: String): String =
            this?.utf8StringOrNull() ?: error("Symfony twig profile $field is not UTF-8 text")

        private companion object {
            val TWIG_PROFILE_CLASSES = setOf("Twig\\Profiler\\Profile", "Twig_Profiler_Profile")
        }
    }
}

data class SymfonyProfilerTwig(
    val renderTimeMs: Double,
    val templateCallCount: Int,
    val blockCallCount: Int,
    val macroCallCount: Int,
    val renderedTemplates: List<SymfonyProfilerTwigTemplate>,
    val root: SymfonyProfilerTwigProfile,
)

data class SymfonyProfilerTwigTemplate(
    val name: String,
    val path: String?,
    val renderCount: Int,
)

data class SymfonyProfilerTwigProfile(
    val template: String,
    val type: String,
    val name: String,
    val durationMs: Double,
    val children: List<SymfonyProfilerTwigProfile>,
)
