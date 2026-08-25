package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpReference
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeException
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeLimits
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializer
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.utf8StringOrNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

private const val PROFILER_MAX_INPUT_BYTES = 25 * 1024 * 1024

/**
 * Parsed raw Symfony profile shared by the individual profiler collector consumers.
 *
 * Keeping the PHP parse in this neutral model lets MCP expose more collector types later without
 * coupling profile loading or request metadata to Doctrine's `db` collector.
 */
class SymfonyProfilerProfile private constructor(
    internal val result: PhpUnserializeResult,
    private val profile: PhpArray,
) {
    val token: String?
        get() = profile["token"].resolve(result)?.utf8StringOrNull()

    val method: String?
        get() = profile["method"].resolve(result)?.utf8StringOrNull()

    val url: String?
        get() = profile["url"].resolve(result)?.utf8StringOrNull()

    val statusCode: Int?
        get() = (profile["status_code"].resolve(result) as? PhpInteger)?.value?.toInt()

    val collectorNames: List<String>
        get() = collectors().entries.mapNotNull { entry ->
            (entry.key as? PhpStringKey)?.bytes?.utf8StringOrNull()
        }

    fun collector(name: String): PhpObject? = collectors()[name].resolve(result) as? PhpObject

    private fun collectors(): PhpArray = profile["data"].resolve(result) as? PhpArray
        ?: error("Symfony profile does not contain a collector array")

    companion object {
        /** Parses raw or GZIP data with fixed defensive limits before collector consumers run. */
        fun read(input: ByteArray): SymfonyProfilerProfile {
            val limits = PhpUnserializeLimits(maxInputBytes = PROFILER_MAX_INPUT_BYTES)
            if (input.size > limits.maxInputBytes) {
                throw PhpUnserializeException(
                    limits.maxInputBytes,
                    null,
                    "input exceeds limit ${limits.maxInputBytes}",
                )
            }

            val serialized = decodeGzip(input, limits.maxInputBytes)
            val result = PhpUnserializer.unserialize(serialized, limits)
            val profile = result.root as? PhpArray
                ?: error("Symfony profile root must be a PHP array")
            check(profile["data"].resolve(result) is PhpArray) {
                "Symfony profile does not contain a collector array"
            }

            return SymfonyProfilerProfile(result, profile)
        }

        private fun decodeGzip(input: ByteArray, maxBytes: Int): ByteArray {
            if (input.size < 3 || input[0] != 0x1f.toByte() || input[1] != 0x8b.toByte() || input[2] != 8.toByte()) {
                return input
            }

            val output = ByteArrayOutputStream(minOf(input.size, maxBytes))
            GZIPInputStream(ByteArrayInputStream(input)).use { gzip ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val read = gzip.read(buffer)
                    if (read < 0) {
                        break
                    }
                    if (read > maxBytes - total) {
                        throw PhpUnserializeException(
                            maxBytes,
                            null,
                            "decompressed input exceeds limit $maxBytes",
                        )
                    }
                    output.write(buffer, 0, read)
                    total += read
                }
            }

            return output.toByteArray()
        }
    }
}

internal fun PhpValue?.resolve(result: PhpUnserializeResult): PhpValue? =
    if (this is PhpReference) result.resolve(this) else this
