package fr.adrienbrault.idea.symfony2plugin.phpUnserializer

import java.nio.charset.StandardCharsets
import java.util.Collections
import kotlin.math.min

/**
 * Passively parses one PHP `serialize()` byte stream without loading PHP classes or executing code.
 *
 * Example:
 * ```kotlin
 * val result = PhpUnserializer.unserialize("a:1:{s:4:\"name\";s:3:\"Ada\";}".encodeToByteArray())
 * val name = result.root.requireArray()["name"]?.utf8StringOrNull()
 * ```
 */
object PhpUnserializer {
    /**
     * Parses exactly one value and rejects trailing bytes. The input is defensively copied once,
     * and [limits] apply only to this call.
     */
    @JvmStatic
    fun unserialize(
        input: ByteArray,
        limits: PhpUnserializeLimits = PhpUnserializeLimits(),
    ): PhpUnserializeResult {
        if (input.size > limits.maxInputBytes) {
            throw PhpUnserializeException(
                limits.maxInputBytes,
                null,
                "input size ${input.size} exceeds limit ${limits.maxInputBytes}",
            )
        }

        return Parser(input.copyOf(), limits).parse()
    }

    private class Parser(
        private val input: ByteArray,
        private val limits: PhpUnserializeLimits,
    ) {
        private var offset = 0
        private var parsedValueCount = 0
        private val referenceSlots = ArrayList<ReferenceSlot>()

        fun parse(): PhpUnserializeResult {
            val root = parseValue(0)
            if (offset != input.size) {
                fail(offset, "trailing bytes after root value", input[offset].asChar())
            }

            val references = referenceSlots.mapIndexed { index, slot ->
                slot.value ?: fail(input.size, "unresolved reference slot ${index + 1}")
            }
            return PhpUnserializeResult(root, references, parsedValueCount)
        }

        private fun parseValue(containerDepth: Int): PhpValue {
            val start = offset
            val tag = readTag()
            parsedValueCount++
            if (parsedValueCount > limits.maxTotalValues) {
                fail(start, "parsed value limit ${limits.maxTotalValues} exceeded", tag)
            }

            return when (tag) {
                'N' -> parseNull(tag)
                'b' -> parseBoolean(tag)
                'i' -> parseInteger(tag)
                'd' -> parseFloat(tag)
                's' -> register(parseString(tag), ReferenceType.VALUE)
                'a' -> parseArray(containerDepth, start, tag)
                'O' -> parseObject(containerDepth, start, tag)
                'E' -> parseEnum(start, tag)
                'r' -> parseReference(tag, PhpReferenceKind.OBJECT)
                'R' -> parseReference(tag, PhpReferenceKind.ALIAS)
                'C' -> parseCustomObject(tag)
                else -> fail(start, "unsupported serialization tag", tag)
            }
        }

        private fun parseNull(tag: Char): PhpValue {
            expect(';', tag)
            return register(PhpNull, ReferenceType.VALUE)
        }

        private fun parseBoolean(tag: Char): PhpValue {
            expect(':', tag)
            val valueOffset = offset
            val value = when (readByte(tag)) {
                '0'.code.toByte() -> false
                '1'.code.toByte() -> true
                else -> fail(valueOffset, "boolean must be 0 or 1", tag)
            }
            expect(';', tag)
            return register(PhpBoolean(value), ReferenceType.VALUE)
        }

        private fun parseInteger(tag: Char): PhpValue {
            expect(':', tag)
            val value = readSignedLong(';', "integer", tag)
            return register(PhpInteger(value), ReferenceType.VALUE)
        }

        private fun parseFloat(tag: Char): PhpValue {
            expect(':', tag)
            val valueOffset = offset
            val end = findDelimiter(';', tag)
            val length = end - valueOffset
            if (length == 0) {
                fail(valueOffset, "double is empty", tag)
            }

            val value = when {
                matchesAscii(valueOffset, length, "INF") -> Double.POSITIVE_INFINITY
                matchesAscii(valueOffset, length, "-INF") -> Double.NEGATIVE_INFINITY
                matchesAscii(valueOffset, length, "NAN") -> Double.NaN
                else -> parseFiniteDouble(valueOffset, length, tag)
            }
            offset = end + 1
            return register(PhpFloat(value), ReferenceType.VALUE)
        }

        private fun parseString(tag: Char): PhpString {
            expect(':', tag)
            val bytes = readQuotedBytes("string length", tag)
            expect(';', tag)
            return PhpString(bytes)
        }

        private fun parseArray(containerDepth: Int, start: Int, tag: Char): PhpArray {
            ensureContainerDepth(containerDepth, start, tag)
            expect(':', tag)
            val countOffset = offset
            val count = readUnsignedInt(':', "array entry count", tag)
            ensureContainerCount(count, countOffset, tag)
            expect('{', tag)

            val slot = reserveSlot(ReferenceType.ARRAY)
            val entries = ArrayList<PhpArrayEntry>(min(count, MAX_INITIAL_CONTAINER_CAPACITY))
            repeat(count) {
                val key = parseArrayKey()
                val value = parseValue(containerDepth + 1)
                entries.add(PhpArrayEntry(key, value))
            }
            expect('}', tag)

            val value = PhpArray(Collections.unmodifiableList(entries))
            slot.value = value
            return value
        }

        private fun parseArrayKey(context: String = "array key"): PhpArrayKey {
            val start = offset
            val tag = readTag()
            return when (tag) {
                'i' -> {
                    expect(':', tag)
                    PhpIntegerKey(readSignedLong(';', "array integer key", tag))
                }

                's' -> {
                    expect(':', tag)
                    val bytes = readQuotedBytes("array key length", tag)
                    expect(';', tag)
                    PhpStringKey(bytes)
                }

                else -> fail(start, "$context must use tag 'i' or 's'", tag)
            }
        }

        private fun parseObject(containerDepth: Int, start: Int, tag: Char): PhpObject {
            ensureContainerDepth(containerDepth, start, tag)
            expect(':', tag)
            val className = readQuotedBytes("object class-name length", tag)
            expect(':', tag)
            val countOffset = offset
            val count = readUnsignedInt(':', "object property count", tag)
            ensureContainerCount(count, countOffset, tag)
            expect('{', tag)

            val slot = reserveSlot(ReferenceType.OBJECT)
            val properties = ArrayList<PhpObjectProperty>(min(count, MAX_INITIAL_CONTAINER_CAPACITY))
            repeat(count) {
                val key = parseArrayKey("object property key")
                properties.add(createPhpObjectProperty(key, parseValue(containerDepth + 1)))
            }
            expect('}', tag)

            val value = PhpObject(className, Collections.unmodifiableList(properties))
            slot.value = value
            return value
        }

        private fun parseEnum(start: Int, tag: Char): PhpEnum {
            expect(':', tag)
            val encoded = readQuotedBytes("enum name length", tag)
            expect(';', tag)
            val separator = encoded.indexOf(':'.code.toByte())
            if (separator <= 0 || separator == encoded.size - 1) {
                fail(start, "enum value must contain a non-empty enum and case name", tag)
            }

            val value = PhpEnum(
                encoded.slice(0, separator),
                encoded.slice(separator + 1, encoded.size - separator - 1),
            )
            return register(value, ReferenceType.OBJECT)
        }

        private fun parseCustomObject(tag: Char): PhpCustomObject {
            expect(':', tag)
            val className = readQuotedBytes("custom-object class-name length", tag)
            expect(':', tag)
            val payloadLengthOffset = offset
            val payloadLength = readUnsignedInt(':', "custom-object payload length", tag)
            ensurePayloadLength(payloadLength, payloadLengthOffset, tag)
            expect('{', tag)
            val payload = readPayload(payloadLength, "custom-object payload", tag)
            expect('}', tag)
            return register(PhpCustomObject(className, payload), ReferenceType.OBJECT)
        }

        // PHP's r token gets a new traversal slot, while R aliases the existing slot directly.
        private fun parseReference(
            tag: Char,
            kind: PhpReferenceKind,
        ): PhpReference {
            expect(':', tag)
            val idOffset = offset
            val id = readUnsignedInt(';', "reference ID", tag)
            if (id == 0 || id > referenceSlots.size) {
                fail(idOffset, "reference ID $id does not identify an earlier value", tag)
            }

            val target = referenceSlots[id - 1]
            if (kind == PhpReferenceKind.OBJECT && target.type != ReferenceType.OBJECT) {
                fail(idOffset, "object reference does not identify an object value", tag)
            }

            val reference = PhpReference(id, kind)
            if (kind == PhpReferenceKind.OBJECT) {
                register(reference, ReferenceType.OBJECT)
            }
            return reference
        }

        private fun readQuotedBytes(field: String, tag: Char): PhpBytes {
            val lengthOffset = offset
            val length = readUnsignedInt(':', field, tag)
            ensurePayloadLength(length, lengthOffset, tag)
            expect('"', tag)
            val bytes = readPayload(length, field, tag)
            expect('"', tag)
            return bytes
        }

        private fun readPayload(length: Int, field: String, tag: Char): PhpBytes {
            if (length > input.size - offset) {
                fail(offset, "$field is truncated", tag)
            }
            val bytes = PhpBytes.view(input, offset, length)
            offset += length
            return bytes
        }

        private fun readUnsignedInt(delimiter: Char, field: String, tag: Char): Int {
            val start = offset
            if (offset >= input.size) {
                fail(offset, "$field is missing", tag)
            }
            if (input[offset] == '-'.code.toByte()) {
                fail(start, "$field must not be negative", tag)
            }

            var result = 0
            var digits = 0
            while (offset < input.size && input[offset] != delimiter.code.toByte()) {
                val digit = input[offset].toInt() - '0'.code
                if (digit !in 0..9) {
                    fail(offset, "$field contains a non-digit", tag)
                }
                if (result > (Int.MAX_VALUE - digit) / 10) {
                    fail(start, "$field overflows an integer", tag)
                }
                result = result * 10 + digit
                digits++
                offset++
            }
            if (digits == 0) {
                fail(start, "$field is empty", tag)
            }
            expect(delimiter, tag)
            return result
        }

        private fun readSignedLong(delimiter: Char, field: String, tag: Char): Long {
            val start = offset
            if (offset >= input.size) {
                fail(offset, "$field is missing", tag)
            }

            var negative = false
            if (input[offset] == '-'.code.toByte() || input[offset] == '+'.code.toByte()) {
                negative = input[offset] == '-'.code.toByte()
                offset++
            }
            val digitStart = offset
            val limit = if (negative) Long.MIN_VALUE else -Long.MAX_VALUE
            val multiplyLimit = limit / 10
            var result = 0L

            while (offset < input.size && input[offset] != delimiter.code.toByte()) {
                val digit = input[offset].toInt() - '0'.code
                if (digit !in 0..9) {
                    fail(offset, "$field contains a non-digit", tag)
                }
                if (result < multiplyLimit) {
                    fail(start, "$field overflows a long", tag)
                }
                result *= 10
                if (result < limit + digit) {
                    fail(start, "$field overflows a long", tag)
                }
                result -= digit
                offset++
            }
            if (offset == digitStart) {
                fail(start, "$field is empty", tag)
            }
            expect(delimiter, tag)
            return if (negative) result else -result
        }

        private fun parseFiniteDouble(start: Int, length: Int, tag: Char): Double {
            var index = start
            val end = start + length
            if (index < end && (input[index] == '+'.code.toByte() || input[index] == '-'.code.toByte())) {
                index++
            }

            var digits = 0
            while (index < end && input[index] in '0'.code.toByte()..'9'.code.toByte()) {
                digits++
                index++
            }
            if (index < end && input[index] == '.'.code.toByte()) {
                index++
                while (index < end && input[index] in '0'.code.toByte()..'9'.code.toByte()) {
                    digits++
                    index++
                }
            }
            if (digits == 0) {
                fail(start, "double has invalid syntax", tag)
            }
            if (index < end && (input[index] == 'e'.code.toByte() || input[index] == 'E'.code.toByte())) {
                index++
                if (index < end && (input[index] == '+'.code.toByte() || input[index] == '-'.code.toByte())) {
                    index++
                }
                val exponentStart = index
                while (index < end && input[index] in '0'.code.toByte()..'9'.code.toByte()) {
                    index++
                }
                if (index == exponentStart) {
                    fail(start, "double exponent is empty", tag)
                }
            }
            if (index != end) {
                fail(index, "double has invalid syntax", tag)
            }

            val text = String(input, start, length, StandardCharsets.US_ASCII)
            val value = text.toDoubleOrNull() ?: fail(start, "double cannot be parsed", tag)
            if (!value.isFinite()) {
                fail(start, "finite double overflows", tag)
            }
            return value
        }

        private fun findDelimiter(delimiter: Char, tag: Char): Int {
            for (index in offset until input.size) {
                if (input[index] == delimiter.code.toByte()) {
                    return index
                }
            }
            fail(input.size, "expected '$delimiter' before end of input", tag)
        }

        private fun matchesAscii(start: Int, length: Int, expected: String): Boolean {
            if (length != expected.length) {
                return false
            }
            for (index in expected.indices) {
                if (input[start + index] != expected[index].code.toByte()) {
                    return false
                }
            }
            return true
        }

        private fun ensureContainerDepth(containerDepth: Int, start: Int, tag: Char) {
            if (containerDepth >= limits.maxNestingDepth) {
                fail(start, "nesting depth limit ${limits.maxNestingDepth} exceeded", tag)
            }
        }

        private fun ensureContainerCount(count: Int, countOffset: Int, tag: Char) {
            if (count > limits.maxContainerEntries) {
                fail(countOffset, "container entry count $count exceeds limit ${limits.maxContainerEntries}", tag)
            }
        }

        private fun ensurePayloadLength(length: Int, lengthOffset: Int, tag: Char) {
            if (length > limits.maxPayloadBytes) {
                fail(lengthOffset, "payload length $length exceeds limit ${limits.maxPayloadBytes}", tag)
            }
        }

        private fun <T : PhpValue> register(value: T, type: ReferenceType): T {
            referenceSlots.add(ReferenceSlot(type, value))
            return value
        }

        private fun reserveSlot(type: ReferenceType): ReferenceSlot {
            val slot = ReferenceSlot(type, null)
            referenceSlots.add(slot)
            return slot
        }

        private fun readTag(): Char {
            if (offset >= input.size) {
                fail(offset, "expected a value")
            }
            return input[offset++].asChar()
        }

        private fun readByte(tag: Char): Byte {
            if (offset >= input.size) {
                fail(offset, "unexpected end of input", tag)
            }
            return input[offset++]
        }

        private fun expect(expected: Char, tag: Char) {
            if (offset >= input.size) {
                fail(offset, "expected '$expected' before end of input", tag)
            }
            if (input[offset] != expected.code.toByte()) {
                fail(offset, "expected '$expected'", tag)
            }
            offset++
        }

        private fun fail(failureOffset: Int, reason: String, tag: Char? = null): Nothing {
            throw PhpUnserializeException(failureOffset, tag, reason)
        }

        private fun Byte.asChar(): Char = (toInt() and 0xff).toChar()

        private data class ReferenceSlot(
            val type: ReferenceType,
            var value: PhpValue?,
        )

        private enum class ReferenceType {
            VALUE,
            ARRAY,
            OBJECT,
        }

        companion object {
            private const val MAX_INITIAL_CONTAINER_CAPACITY = 1024
        }
    }
}
