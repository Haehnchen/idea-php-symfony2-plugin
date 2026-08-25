package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader

/** Reads the summary and message rows shown by Symfony's translation profiler panel. */
object SymfonyProfilerTranslationConsumer {
    private val TRANSLATION_COLLECTORS = setOf(
        "Symfony\\Component\\Translation\\DataCollector\\TranslationDataCollector",
        "Symfony\\Bundle\\FrameworkBundle\\DataCollector\\TranslationDataCollector",
    )

    fun read(profile: SymfonyProfilerProfile): SymfonyProfilerTranslation {
        val collector = profile.collector("translation")
            ?: error("Symfony profile does not contain the translation collector")
        check(collector.className.utf8StringOrNull() in TRANSLATION_COLLECTORS) {
            "Symfony translation collector has an unsupported class"
        }

        val rawData = collector["data"]
            ?: error("Symfony translation collector does not contain its data value")
        val data = SymfonyVarDumperDataReader(profile.result).read(rawData) as? ProfilerArray
            ?: error("Symfony translation collector data is not an array")
        val messages = readMessages(data["messages"])

        return SymfonyProfilerTranslation(
            locale = data.text("locale"),
            fallbackLocales = readStringList(data["fallback_locales"]),
            definedCount = data.stateCount(SymfonyProfilerTranslationState.DEFINED, messages),
            missingCount = data.stateCount(SymfonyProfilerTranslationState.MISSING, messages),
            fallbackCount = data.stateCount(SymfonyProfilerTranslationState.FALLBACK, messages),
            messages = messages,
        )
    }

    private fun readMessages(value: ProfilerValue?): List<SymfonyProfilerTranslationMessage> {
        val messages = value as? ProfilerArray ?: return emptyList()
        return messages.entries.mapNotNull { entry ->
            val message = entry.value as? ProfilerArray ?: return@mapNotNull null
            SymfonyProfilerTranslationMessage(
                locale = message.text("locale"),
                fallbackLocale = message.text("fallbackLocale") ?: message.text("fallback_locale"),
                domain = message.text("domain"),
                id = message.text("id"),
                translation = message.text("translation"),
                state = SymfonyProfilerTranslationState.from(message["state"]),
                count = (message["count"] as? ProfilerInteger)?.value?.coerceAtLeast(1) ?: 1,
            )
        }
    }

    private fun readStringList(value: ProfilerValue?): List<String> = (value as? ProfilerArray)
        ?.entries
        ?.mapNotNull { entry -> (entry.value as? ProfilerString)?.utf8StringOrNull() }
        .orEmpty()

    private fun ProfilerArray.stateCount(
        state: SymfonyProfilerTranslationState,
        messages: List<SymfonyProfilerTranslationMessage>,
    ): Int {
        val count = entries.firstOrNull { entry ->
            (entry.key as? PhpIntegerKey)?.value == state.code.toLong()
        }?.value as? ProfilerInteger

        return count?.value
            ?.coerceIn(0, Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: messages.count { it.state == state }
    }

    private fun ProfilerArray.text(key: String): String? =
        (this[key] as? ProfilerString)?.utf8StringOrNull()
}

data class SymfonyProfilerTranslation(
    val locale: String?,
    val fallbackLocales: List<String>,
    val definedCount: Int,
    val missingCount: Int,
    val fallbackCount: Int,
    val messages: List<SymfonyProfilerTranslationMessage>,
)

data class SymfonyProfilerTranslationMessage(
    val locale: String?,
    val fallbackLocale: String?,
    val domain: String?,
    val id: String?,
    val translation: String?,
    val state: SymfonyProfilerTranslationState,
    val count: Long,
)

enum class SymfonyProfilerTranslationState(
    val code: Int,
    val csvValue: String,
    internal val displayOrder: Int,
) {
    DEFINED(0, "defined", 2),
    MISSING(1, "missing", 0),
    FALLBACK(2, "fallback", 1),
    ;

    companion object {
        internal fun from(value: ProfilerValue?): SymfonyProfilerTranslationState = when (value) {
            is ProfilerInteger -> entries.firstOrNull { it.code.toLong() == value.value } ?: DEFINED
            is ProfilerString -> value.utf8StringOrNull()?.let { state ->
                entries.firstOrNull { it.csvValue.equals(state, ignoreCase = true) }
            } ?: DEFINED
            else -> DEFINED
        }
    }
}
