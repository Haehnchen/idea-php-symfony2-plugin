package fr.adrienbrault.idea.symfony2plugin.tests.php.unserializer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBoolean
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpNull
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpString
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializer
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.requireArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.utf8StringOrNull
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets

class PhpUnserializerTest {
    @Test
    fun `parses scalar values and exact long boundaries`() {
        assertSame(PhpNull, parse("N;"))
        assertEquals(PhpBoolean(false), parse("b:0;"))
        assertEquals(PhpBoolean(true), parse("b:1;"))
        assertEquals(PhpInteger(-42), parse("i:-42;"))
        assertEquals(PhpInteger(42), parse("i:+42;"))
        assertEquals(PhpInteger(Long.MIN_VALUE), parse("i:${Long.MIN_VALUE};"))
        assertEquals(PhpInteger(Long.MAX_VALUE), parse("i:${Long.MAX_VALUE};"))
    }

    @Test
    fun `parses empty strings arrays and unknown objects`() {
        assertEquals(0, (parse("s:0:\"\";") as PhpString).bytes.size)
        assertTrue(parse("a:0:{}").requireArray().entries.isEmpty())
        assertTrue((parse("O:7:\"Unknown\":0:{}") as PhpObject).properties.isEmpty())
    }

    @Test
    fun `parses finite and special doubles`() {
        assertEquals(1.25e20, (parse("d:1.25E+20;") as PhpFloat).value)
        assertEquals(1.5, (parse("d:+1.5;") as PhpFloat).value)
        assertEquals(Double.POSITIVE_INFINITY, (parse("d:INF;") as PhpFloat).value)
        assertEquals(Double.NEGATIVE_INFINITY, (parse("d:-INF;") as PhpFloat).value)
        assertTrue((parse("d:NAN;") as PhpFloat).value.isNaN())
        assertEquals(
            java.lang.Double.doubleToRawLongBits(-0.0),
            java.lang.Double.doubleToRawLongBits((parse("d:-0;") as PhpFloat).value),
        )
    }

    @Test
    fun `uses declared byte lengths for UTF-8 and binary strings`() {
        val utf8 = parse("s:2:\"ä\";") as PhpString
        assertEquals("ä", utf8.bytes.utf8String())

        val payload = byteArrayOf(0, '"'.code.toByte(), ';'.code.toByte(), '}'.code.toByte(), 0xff.toByte())
        val serialized = concat(
            "s:${payload.size}:\"".toByteArray(StandardCharsets.US_ASCII),
            payload,
            "\";".toByteArray(StandardCharsets.US_ASCII),
        )
        val binary = PhpUnserializer.unserialize(serialized).root as PhpString
        assertArrayEquals(payload, binary.bytes.toByteArray())
        assertNull(binary.bytes.utf8StringOrNull())
        assertThrows(CharacterCodingException::class.java) { binary.bytes.utf8String() }
    }

    @Test
    fun `retains mixed sparse keys in insertion order and offers readable lookup`() {
        val array = parse("a:3:{i:7;s:5:\"seven\";s:4:\"name\";s:5:\"value\";i:-2;N;}").requireArray()

        assertEquals(
            listOf(PhpIntegerKey(7), PhpStringKey(PhpBytes("name".toByteArray())), PhpIntegerKey(-2)),
            array.entries.map { it.key },
        )
        assertEquals("seven", array[7]?.utf8StringOrNull())
        assertEquals("value", array["name"]?.utf8StringOrNull())
        assertSame(PhpNull, array[-2])
        assertNull(array[0])
    }

    @Test
    fun `lookup indexes preserve the first duplicate key and ordered entries`() {
        val serializedArray = buildString {
            append("a:20:{s:1:\"x\";i:1;s:1:\"x\";i:2;i:7;i:3;i:7;i:4;")
            repeat(16) { index -> append("i:").append(100 + index).append(";N;") }
            append('}')
        }
        val array = parse(serializedArray).requireArray()

        assertEquals(20, array.entries.size)
        assertEquals(PhpInteger(1), array["x"])
        assertEquals(PhpInteger(3), array[7])
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (array.entries as MutableList<Any?>).clear()
        }

        val serializedObject = buildString {
            append("O:1:\"X\":17:{s:1:\"x\";i:1;s:4:\"\u0000*\u0000x\";i:2;")
            repeat(15) { index -> append("i:").append(index).append(";N;") }
            append('}')
        }
        val objectValue = parse(serializedObject) as PhpObject
        assertEquals(PhpInteger(1), objectValue["x"])
    }

    @Test
    fun `takes one defensive input snapshot and returns byte copies`() {
        val input = "s:6:\"secret\";".toByteArray(StandardCharsets.US_ASCII)
        val value = PhpUnserializer.unserialize(input).root as PhpString
        input.fill(0)

        assertEquals("secret", value.bytes.utf8String())
        val returned = value.bytes.toByteArray()
        returned.fill(0)
        assertEquals("secret", value.bytes.utf8String())
    }

    @Test
    fun `compacts a parsed byte view for long-term retention`() {
        val parsed = (parse("s:6:\"secret\";") as PhpString).bytes
        val compact = parsed.compact()

        assertEquals(parsed, compact)
        assertNotSame(parsed, compact)
        assertSame(compact, compact.compact())
    }

    @Test
    fun `byte values use content equality and safe summaries`() {
        val first = PhpBytes(byteArrayOf(1, 2, 3))
        val second = PhpBytes(byteArrayOf(1, 2, 3))
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, PhpBytes(byteArrayOf(1, 2)))
        assertEquals("PhpBytes(size=3)", first.toString())

        val result = PhpUnserializer.unserialize("s:12:\"private-text\";".toByteArray())
        assertFalse(result.toString().contains("private-text"))
        assertFalse(result.root.toString().contains("private-text"))
    }

    @Test
    fun `result exposes graph counts without exposing its values`() {
        val result = PhpUnserializer.unserialize("a:2:{i:0;i:1;i:1;s:1:\"x\";}".toByteArray())
        assertTrue(result.root is PhpArray)
        assertEquals(3, result.parsedValueCount)
        assertEquals(3, result.referenceCount)
        assertEquals("PhpUnserializeResult(root=PhpArray, referenceCount=3, parsedValueCount=3)", result.toString())
    }

    private fun parse(serialized: String) =
        PhpUnserializer.unserialize(serialized.toByteArray(StandardCharsets.UTF_8)).root

    private fun concat(vararg arrays: ByteArray): ByteArray {
        val result = ByteArray(arrays.sumOf { it.size })
        var offset = 0
        arrays.forEach {
            it.copyInto(result, offset)
            offset += it.size
        }
        return result
    }
}
