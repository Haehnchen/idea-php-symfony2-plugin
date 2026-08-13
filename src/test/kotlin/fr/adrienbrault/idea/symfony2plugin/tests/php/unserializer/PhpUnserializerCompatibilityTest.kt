package fr.adrienbrault.idea.symfony2plugin.tests.php.unserializer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBoolean
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpCustomObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpEnum
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpNull
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpPropertyVisibility
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpReferenceKind
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpString
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializer
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.requireArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.requireObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.requireReference
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.utf8StringOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhpUnserializerCompatibilityTest {
    @Test
    fun `PHP generated corpus covers every required value kind`() {
        val result = PhpUnserializer.unserialize(fixture("all-tags"))
        val kinds = collectKinds(result.root)

        listOf(
            PhpNull::class,
            PhpBoolean::class,
            PhpInteger::class,
            PhpFloat::class,
            PhpString::class,
            PhpArray::class,
            PhpObject::class,
            PhpEnum::class,
            PhpCustomObject::class,
        ).forEach { assertTrue(kinds.contains(it), "missing ${it.simpleName}") }

        val root = result.root.requireArray()
        val binary = root["binary"] as PhpString
        assertEquals(11, binary.bytes.size)
        assertEquals(null, binary.bytes.utf8StringOrNull())
        assertTrue((root["infinity"] as PhpFloat).value.isInfinite())
        assertTrue((root["not_a_number"] as PhpFloat).value.isNaN())
    }

    @Test
    fun `retains public protected and private property metadata`() {
        val value = PhpUnserializer.unserialize(fixture("visibility")).root.requireObject()
        assertEquals("NeutralVisibility", value.className.utf8String())
        assertEquals(
            listOf(PhpPropertyVisibility.PUBLIC, PhpPropertyVisibility.PROTECTED, PhpPropertyVisibility.PRIVATE),
            value.properties.map { it.visibility },
        )
        assertEquals(
            listOf("publicValue", "protectedValue", "privateValue"),
            value.properties.map { it.logicalName?.utf8String() })
        assertEquals(null, value.properties[0].declaringClass)
        assertEquals(null, value.properties[1].declaringClass)
        assertEquals("NeutralVisibility", value.properties[2].declaringClass?.utf8String())
        assertTrue(
            requireNotNull(value.properties[1].name).toByteArray().copyOfRange(0, 3)
                .contentEquals(byteArrayOf(0, '*'.code.toByte(), 0)),
        )
        assertEquals("private", value["privateValue"]?.utf8StringOrNull())
    }

    @Test
    fun `retains integer object property keys emitted by PHP serialize hooks`() {
        val value = PhpUnserializer.unserialize(fixture("integer-properties")).root.requireObject()

        assertEquals("seven", value[7]?.utf8StringOrNull())
        assertEquals("value", value["name"]?.utf8StringOrNull())
        assertEquals(null, value.properties[0].name)
        assertEquals(null, value.properties[0].visibility)
    }

    @Test
    fun `resolves object self references without creating recursive Kotlin objects`() {
        val result = PhpUnserializer.unserialize(fixture("object-cycle"))
        val root = result.root.requireObject()
        val self = root["self"]?.requireReference()

        assertEquals(PhpReferenceKind.OBJECT, self?.kind)
        assertSame(root, result.resolve(requireNotNull(self)))
    }

    @Test
    fun `keeps aliases distinct and resolves them to the shared scalar`() {
        val result = PhpUnserializer.unserialize(fixture("aliases"))
        val root = result.root.requireArray()
        val alias = root[1]?.requireReference()

        assertEquals(PhpReferenceKind.ALIAS, alias?.kind)
        assertEquals("shared", (result.resolve(requireNotNull(alias)) as PhpString).bytes.utf8String())
        assertEquals(2, result.referenceCount)
    }

    @Test
    fun `matches PHP traversal numbering where object references consume slots`() {
        val result = PhpUnserializer.unserialize(fixture("reference-order"))
        val root = result.root.requireArray()
        val firstReference = root[1]?.requireReference()
        val secondReference = root[3]?.requireReference()

        assertEquals(2, firstReference?.id)
        assertEquals(6, secondReference?.id)
        assertSame(root[0], result.resolve(requireNotNull(firstReference)))
        assertSame(root[2], result.resolve(requireNotNull(secondReference)))
    }

    @Test
    fun `treats enums as object-reference targets`() {
        val result = PhpUnserializer.unserialize(fixture("enum-reference"))
        val root = result.root.requireArray()
        val enum = assertInstanceOf(PhpEnum::class.java, root[0])
        val reference = root[1]?.requireReference()

        assertEquals("NeutralState", enum.enumName.utf8String())
        assertEquals("Ready", enum.caseName.utf8String())
        assertSame(enum, result.resolve(requireNotNull(reference)))
    }

    @Test
    fun `preserves a custom-object payload and resumes at its exact length`() {
        val root = PhpUnserializer.unserialize(fixture("custom-followed")).root.requireArray()
        val custom = assertInstanceOf(PhpCustomObject::class.java, root[0])

        assertEquals("NeutralLegacy", custom.className.utf8String())
        assertTrue(custom.payload.toByteArray().contains(0))
        assertEquals(PhpInteger(42), root[1])
    }

    @Test
    fun `parses large generated arrays in one complete pass`() {
        val entryCount = 10_000
        val serialized = buildString(entryCount * 20) {
            append("a:").append(entryCount).append(":{")
            repeat(entryCount) { index ->
                append("i:").append(index).append(";i:").append(-index).append(';')
            }
            append('}')
        }.toByteArray()

        val result = PhpUnserializer.unserialize(serialized)
        val root = result.root.requireArray()
        assertEquals(entryCount, root.entries.size)
        assertEquals(PhpInteger(-(entryCount - 1).toLong()), root[(entryCount - 1).toLong()])
        assertEquals(entryCount + 1, result.parsedValueCount)
    }

    private fun fixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/unserializer/generated/$name.ser"),
    ).use { it.readAllBytes() }

    private fun collectKinds(root: PhpValue): Set<kotlin.reflect.KClass<out PhpValue>> {
        val kinds = linkedSetOf<kotlin.reflect.KClass<out PhpValue>>()
        fun visit(value: PhpValue) {
            kinds.add(value::class)
            when (value) {
                is PhpArray -> value.entries.forEach { visit(it.value) }
                is PhpObject -> value.properties.forEach { visit(it.value) }
                else -> Unit
            }
        }
        visit(root)
        return kinds
    }
}
