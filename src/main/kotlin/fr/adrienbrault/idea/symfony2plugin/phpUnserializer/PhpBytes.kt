package fr.adrienbrault.idea.symfony2plugin.phpUnserializer

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * An immutable byte string.
 *
 * Parsed instances are views into the unserializer's private input snapshot. Retaining one of
 * those instances therefore retains that snapshot. Public byte-array access always returns a
 * copy, so neither the caller's original input nor a returned array can mutate the value graph.
 */
class PhpBytes private constructor(
    private val storage: ByteArray,
    private val offset: Int,
    val size: Int,
) {
    constructor(bytes: ByteArray) : this(bytes.copyOf(), 0, bytes.size)

    /** Decodes with strict UTF-8 rules and reports malformed or unmappable bytes. */
    @Throws(CharacterCodingException::class)
    fun utf8String(): String = StandardCharsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(storage, offset, size))
        .toString()

    /** Returns strict UTF-8 text, or `null` when this is a binary string. */
    fun utf8StringOrNull(): String? = try {
        utf8String()
    } catch (_: CharacterCodingException) {
        null
    }

    /** Returns a mutable copy; changing it cannot affect the parsed graph. */
    fun toByteArray(): ByteArray = storage.copyOfRange(offset, offset + size)

    /**
     * Returns an equal byte string backed only by its own bytes.
     *
     * Use this when retaining a small parsed value after discarding the larger parse result;
     * parsed views otherwise keep the unserializer's complete input snapshot alive.
     */
    fun compact(): PhpBytes {
        if (offset == 0 && size == storage.size) {
            return this
        }

        return PhpBytes(storage.copyOfRange(offset, offset + size), 0, size)
    }

    fun contentEquals(other: ByteArray): Boolean {
        if (size != other.size) {
            return false
        }

        for (index in 0 until size) {
            if (storage[offset + index] != other[index]) {
                return false
            }
        }

        return true
    }

    internal fun byteAt(index: Int): Byte {
        require(index in 0 until size) { "Byte index out of bounds" }
        return storage[offset + index]
    }

    internal fun indexOf(value: Byte, fromIndex: Int = 0): Int {
        require(fromIndex in 0..size) { "Byte index out of bounds" }
        for (index in fromIndex until size) {
            if (storage[offset + index] == value) {
                return index
            }
        }

        return -1
    }

    internal fun slice(sliceOffset: Int, sliceLength: Int): PhpBytes {
        require(sliceOffset >= 0 && sliceLength >= 0 && sliceOffset <= size - sliceLength) {
            "Byte slice out of bounds"
        }
        return PhpBytes(storage, offset + sliceOffset, sliceLength)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PhpBytes || size != other.size) {
            return false
        }

        for (index in 0 until size) {
            if (storage[offset + index] != other.storage[other.offset + index]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = 1
        for (index in 0 until size) {
            result = 31 * result + storage[offset + index]
        }
        return result
    }

    override fun toString(): String = "PhpBytes(size=$size)"

    companion object {
        internal fun view(storage: ByteArray, offset: Int, length: Int): PhpBytes =
            PhpBytes(storage, offset, length)

        internal fun utf8(value: String): PhpBytes {
            val bytes = value.toByteArray(StandardCharsets.UTF_8)
            return PhpBytes(bytes, 0, bytes.size)
        }
    }
}
