package fr.adrienbrault.idea.symfony2plugin.tests.php.unserializer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeException
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeLimits
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class PhpUnserializerMalformedInputTest {
    @Test
    fun `rejects every truncation of a representative nested value`() {
        val valid = "a:2:{s:4:\"node\";O:7:\"Unknown\":1:{s:4:\"name\";s:5:\"value\";}i:1;C:6:\"Legacy\":5:{a;}\";}}"
            .toByteArray(StandardCharsets.US_ASCII)

        for (length in valid.indices) {
            assertThrows(
                PhpUnserializeException::class.java,
                { PhpUnserializer.unserialize(valid.copyOf(length)) },
                "prefix length $length",
            )
        }
        PhpUnserializer.unserialize(valid)
    }

    @Test
    fun `reports an unknown nested tag at its exact offset`() {
        val serialized = "a:1:{i:0;X:7;}"
        val exception = assertFailure(serialized)

        assertEquals(serialized.indexOf('X'), exception.offset)
        assertEquals('X', exception.tag)
        assertTrue(exception.reason.contains("unsupported"))
    }

    @Test
    fun `reports a malformed enum at its tag offset`() {
        val serialized = "a:1:{i:0;E:7:\"NoColon\";}"
        val exception = assertFailure(serialized)

        assertEquals(serialized.indexOf('E'), exception.offset)
        assertEquals('E', exception.tag)
    }

    @Test
    fun `rejects malformed delimiters counts lengths keys and trailing bytes`() {
        listOf(
            "N:",
            "b:2;",
            "i:1:",
            "s:-1:\"\";",
            "s:2147483648:\"\";",
            "s:2:\"x\";",
            "a:-1:{}",
            "a:2147483648:{}",
            "a:1:{b:0;N;}",
            "a:1:{i:0;N;",
            "a:0:{}x",
            "O:1:\"X\":1:{b:0;N;}",
            "C:1:\"X\":2:{x}",
            "E:7:\"NoColon\";",
            "S:1:\"x\";",
        ).forEach { assertFailure(it) }
    }

    @Test
    fun `rejects integer and double overflow or invalid numeric syntax`() {
        listOf(
            "i:9223372036854775808;",
            "i:-9223372036854775809;",
            "i:--;",
            "d:;",
            "d:1e;",
            "d:1.2.3;",
            "d:1e9999;",
            "d:Infinity;",
            "d:nan;",
        ).forEach { assertFailure(it) }
    }

    @Test
    fun `validates reference IDs and object-reference kinds`() {
        listOf(
            "R:0;",
            "R:1;",
            "a:1:{i:0;R:3;}",
            "a:2:{i:0;i:1;i:1;r:2;}",
        ).forEach { assertFailure(it) }
    }

    @Test
    fun `enforces every configured limit before descent or allocation`() {
        assertFailure("N;", PhpUnserializeLimits(maxTotalValues = 0))
        assertFailure("a:0:{}", PhpUnserializeLimits(maxNestingDepth = 0))
        assertFailure("a:1:{i:0;N;}", PhpUnserializeLimits(maxContainerEntries = 0))
        assertFailure("s:2:\"ab\";", PhpUnserializeLimits(maxPayloadBytes = 1))
        assertFailure("N;", PhpUnserializeLimits(maxInputBytes = 1))

        val threeLevels = nestedArrays(3)
        PhpUnserializer.unserialize(threeLevels.toByteArray(), PhpUnserializeLimits(maxNestingDepth = 3))
        assertFailure(nestedArrays(4), PhpUnserializeLimits(maxNestingDepth = 3))
    }

    @Test
    fun `limits apply to one static invocation only`() {
        val serialized = "s:2:\"ab\";"
        assertFailure(serialized, PhpUnserializeLimits(maxPayloadBytes = 1))
        PhpUnserializer.unserialize(serialized.toByteArray(StandardCharsets.US_ASCII))
    }

    @Test
    fun `error messages never include serialized payload context`() {
        val sensitive = "Xprivate-value-that-must-not-appear"
        val exception = assertFailure(sensitive)
        assertFalse(exception.message.orEmpty().contains("private-value"))
        assertEquals(0, exception.offset)
    }

    private fun assertFailure(
        serialized: String,
        limits: PhpUnserializeLimits = PhpUnserializeLimits(),
    ): PhpUnserializeException = assertThrows(PhpUnserializeException::class.java) {
        PhpUnserializer.unserialize(serialized.toByteArray(StandardCharsets.US_ASCII), limits)
    }

    private fun nestedArrays(depth: Int): String {
        var value = "N;"
        repeat(depth) {
            value = "a:1:{i:0;$value}"
        }
        return value
    }
}
