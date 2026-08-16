package fr.adrienbrault.idea.symfony2plugin.profiler.decoder

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import java.nio.charset.StandardCharsets

/** A collector-neutral value tree produced after PHP-specific structures have been decoded. */
sealed interface ProfilerValue

data object ProfilerNull : ProfilerValue

data class ProfilerBoolean(val value: Boolean) : ProfilerValue

data class ProfilerInteger(val value: Long) : ProfilerValue

data class ProfilerFloat(val value: Double) : ProfilerValue

data class ProfilerString(val bytes: PhpBytes) : ProfilerValue {
    fun utf8StringOrNull(): String? = bytes.utf8StringOrNull()
}

data class ProfilerEntry(
    val key: PhpArrayKey,
    val value: ProfilerValue,
)

/** Keeps PHP array order and mixed integer/string keys without coercing them into a Kotlin map. */
data class ProfilerArray(val entries: List<ProfilerEntry>) : ProfilerValue {
    operator fun get(key: String): ProfilerValue? {
        val bytes = PhpBytes(key.toByteArray(StandardCharsets.UTF_8))
        return entries.firstOrNull { entry ->
            (entry.key as? PhpStringKey)?.bytes == bytes
        }?.value
    }
}

data class ProfilerEnum(
    val enumName: PhpBytes,
    val caseName: PhpBytes,
) : ProfilerValue

/** Represents a recursive VarDumper position without constructing a cyclic Kotlin graph. */
data class ProfilerReference(val position: Long) : ProfilerValue

/** Opaque serialized payloads are described but never copied into profiler output. */
data class ProfilerOpaque(
    val kind: String,
    val payloadBytes: Int? = null,
) : ProfilerValue

internal fun profilerString(value: String): ProfilerString =
    ProfilerString(PhpBytes(value.toByteArray(StandardCharsets.UTF_8)))
