package fr.adrienbrault.idea.symfony2plugin.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslation
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslationConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslationMessage

private const val MAX_CSV_VALUE_LENGTH = 1_000
private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]+")

/** Renders Symfony translation counts and a paginated CSV message list. */
internal object SymfonyProfilerTranslationDetailRenderer : ProfilerDetailRenderer {
    override val name = "translation"
    override val overviewWeight = 80

    override fun renderOverview(profile: SymfonyProfilerProfile): String =
        formatOverview(SymfonyProfilerTranslationConsumer.read(profile))

    override fun renderDetails(profile: SymfonyProfilerProfile, page: Int): String =
        formatDetails(SymfonyProfilerTranslationConsumer.read(profile), page)

    /** Mirrors the stable metrics from Symfony's translation profiler panel. */
    internal fun formatOverview(translation: SymfonyProfilerTranslation): String = buildString {
        appendSummary(translation)
    }.trimEnd()

    /** Renders actionable states first and keeps every escaped CSV row on one detail page. */
    internal fun formatDetails(translation: SymfonyProfilerTranslation, page: Int = 1): String {
        val rows = translation.messages
            .withIndex()
            .sortedWith(
                compareBy<IndexedValue<SymfonyProfilerTranslationMessage>> { it.value.state.displayOrder }
                    .thenBy { it.index },
            )
            .map { formatCsvRow(it.value) }
        val detailPage = paginateProfilerDetailEntries(rows, page)
        val heading = if (detailPage.isPaginated) {
            "### Messages (CSV, page ${detailPage.number} of ${detailPage.total})"
        } else {
            "### Messages (CSV)"
        }

        return buildString {
            appendSummary(translation)
            appendLine()
            appendLine(heading)
            appendLine()
            if (detailPage.entries.isEmpty()) {
                appendLine("No translation messages recorded.")
            } else {
                appendLine("state,locale,fallback_locale,domain,count,id,translation")
                detailPage.entries.forEach { row -> appendLine(row) }
            }
        }.trimEnd()
    }

    private fun StringBuilder.appendSummary(translation: SymfonyProfilerTranslation) {
        appendLine("## Collector: translation")
        appendLine()
        appendLine("- Default locale: ${plainText(translation.locale ?: "none")}")
        appendLine(
            "- Fallback locales: " +
                if (translation.fallbackLocales.isEmpty()) {
                    "none"
                } else {
                    translation.fallbackLocales.joinToString { plainText(it) }
                },
        )
        appendLine("- Defined messages: ${translation.definedCount}")
        appendLine("- Missing messages: ${translation.missingCount}")
        appendLine("- Fallback messages: ${translation.fallbackCount}")
    }
}

private fun formatCsvRow(message: SymfonyProfilerTranslationMessage): String = listOf(
    message.state.csvValue,
    message.locale.orEmpty(),
    message.fallbackLocale.orEmpty(),
    message.domain.orEmpty(),
    message.count.toString(),
    message.id.orEmpty(),
    message.translation.orEmpty(),
).joinToString(",") { value -> csvValue(value) }

/** Produces bounded single-line RFC 4180 fields for an untrusted profiler value. */
private fun csvValue(value: String): String {
    val normalized = value.replace(CONTROL_CHARACTERS, " ")
    val bounded = if (normalized.length <= MAX_CSV_VALUE_LENGTH) {
        normalized
    } else {
        normalized.take(MAX_CSV_VALUE_LENGTH) + "… [truncated]"
    }
    return if (',' in bounded || '"' in bounded) {
        "\"${bounded.replace("\"", "\"\"")}\""
    } else {
        bounded
    }
}

private fun plainText(value: String): String = value
    .replace(CONTROL_CHARACTERS, " ")
    .replace("|", "\\|")
