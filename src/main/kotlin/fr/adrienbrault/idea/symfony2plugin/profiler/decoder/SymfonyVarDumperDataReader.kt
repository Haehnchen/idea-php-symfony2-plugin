package fr.adrienbrault.idea.symfony2plugin.profiler.decoder

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBoolean
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpCustomObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpEnum
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpNull
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpReference
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpString
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue

/**
 * Expands Symfony VarDumper `Data` values after the lossless PHP unserializer has run.
 *
 * Descriptor arrays are interpreted only inside a confirmed VarDumper data table. Ordinary PHP
 * arrays therefore keep their original meaning when this reader is used for older collectors.
 */
class SymfonyVarDumperDataReader(
    private val result: PhpUnserializeResult,
) {
    fun read(value: PhpValue?): ProfilerValue = when (val resolved = resolve(value)) {
        is PhpObject -> if (resolved.isVarDumperData()) readData(resolved) else convertObject(resolved, mutableSetOf())
        else -> convertPlain(resolved, mutableSetOf())
    }

    private fun readData(dataObject: PhpObject): ProfilerValue {
        val levels = resolve(dataObject["data"]) as? PhpArray
            ?: error("Symfony VarDumper Data does not contain its data table")
        val position = (resolve(dataObject["position"]) as? PhpInteger)?.value ?: 0L
        val key = when (val rawKey = resolve(dataObject["key"])) {
            null -> PhpIntegerKey(0)
            is PhpInteger -> PhpIntegerKey(rawKey.value)
            is PhpString -> PhpStringKey(rawKey.bytes)
            else -> error("Symfony VarDumper Data has an unsupported cursor key")
        }
        val root = level(levels, position).value(key)
            ?: error("Symfony VarDumper Data cursor does not identify a value")

        return expandDataItem(root, levels, mutableSetOf(), mutableSetOf())
    }

    private fun expandDataItem(
        value: PhpValue,
        levels: PhpArray,
        activePositions: MutableSet<Long>,
        activeReferences: MutableSet<Int>,
    ): ProfilerValue {
        if (value is PhpReference) {
            if (!activeReferences.add(value.id)) {
                return ProfilerReference(value.id.toLong())
            }
            return try {
                expandDataItem(result.resolve(value), levels, activePositions, activeReferences)
            } finally {
                activeReferences.remove(value.id)
            }
        }

        val stub = dataStub(value)
        if (stub == null) {
            return convertPlain(value, activeReferences)
        }

        // Reference stubs without their own position wrap another scalar or stub value.
        if (stub.type == TYPE_REFERENCE && stub.position == 0L) {
            return stub.value?.let { expandDataItem(it, levels, activePositions, activeReferences) }
                ?: ProfilerNull
        }
        if (stub.type == TYPE_STRING) {
            return convertPlain(stub.value, activeReferences)
        }
        if (stub.position == 0L) {
            return ProfilerArray(emptyList())
        }
        if (!activePositions.add(stub.position)) {
            return ProfilerReference(stub.position)
        }

        return try {
            val children = level(levels, stub.position)
            ProfilerArray(
                children.entries.map { entry ->
                    ProfilerEntry(
                        compactKey(entry.key),
                        expandDataItem(entry.value, levels, activePositions, activeReferences),
                    )
                },
            )
        } finally {
            activePositions.remove(stub.position)
        }
    }

    private fun dataStub(value: PhpValue): DataStub? = when (value) {
        is PhpArray -> {
            if (value.entries.isEmpty()) {
                null
            } else {
                // VarDumper encodes non-empty arrays as [array kind => child position].
                val position = resolve(value.entries.last().value) as? PhpInteger
                    ?: error("Malformed Symfony VarDumper array descriptor")
                DataStub(TYPE_ARRAY, position.value, null)
            }
        }
        is PhpObject -> {
            if (!value.isVarDumperStub()) {
                error("Unsupported object in Symfony VarDumper data table")
            }
            DataStub(
                type = (resolve(value["type"]) as? PhpInteger)?.value?.toInt() ?: TYPE_REFERENCE,
                position = (resolve(value["position"]) as? PhpInteger)?.value ?: 0L,
                value = value["value"],
            )
        }
        else -> null
    }

    private fun convertPlain(value: PhpValue?, activeReferences: MutableSet<Int>): ProfilerValue = when (value) {
        null, PhpNull -> ProfilerNull
        is PhpReference -> {
            if (!activeReferences.add(value.id)) {
                ProfilerReference(value.id.toLong())
            } else {
                try {
                    convertPlain(result.resolve(value), activeReferences)
                } finally {
                    activeReferences.remove(value.id)
                }
            }
        }
        is PhpBoolean -> ProfilerBoolean(value.value)
        is PhpInteger -> ProfilerInteger(value.value)
        is PhpFloat -> ProfilerFloat(value.value)
        is PhpString -> ProfilerString(value.bytes.compact())
        is PhpArray -> ProfilerArray(
            value.entries.map { entry ->
                ProfilerEntry(compactKey(entry.key), convertPlain(entry.value, activeReferences))
            },
        )
        is PhpObject -> if (value.isVarDumperData()) readData(value) else convertObject(value, activeReferences)
        is PhpEnum -> ProfilerEnum(value.enumName.compact(), value.caseName.compact())
        is PhpCustomObject -> ProfilerOpaque("custom PHP object", value.payload.size)
    }

    private fun convertObject(value: PhpObject, activeReferences: MutableSet<Int>): ProfilerArray = ProfilerArray(
        value.properties.map { property ->
            ProfilerEntry(compactKey(property.key), convertPlain(property.value, activeReferences))
        },
    )

    private fun level(levels: PhpArray, position: Long): PhpArray =
        resolve(levels[position]) as? PhpArray
            ?: error("Symfony VarDumper child position $position is missing")

    private fun PhpArray.value(key: PhpArrayKey): PhpValue? = when (key) {
        is PhpIntegerKey -> this[key.value]
        is PhpStringKey -> this[key.bytes]
    }

    private fun resolve(value: PhpValue?): PhpValue? =
        if (value is PhpReference) result.resolve(value) else value

    private fun PhpObject.isVarDumperData(): Boolean =
        className.utf8StringOrNull() == DATA_CLASS

    private fun PhpObject.isVarDumperStub(): Boolean {
        val name = className.utf8StringOrNull() ?: return false
        return name == STUB_CLASS || name.startsWith(VAR_DUMPER_NAMESPACE) && name.endsWith("Stub")
    }

    private fun compactKey(key: PhpArrayKey): PhpArrayKey = when (key) {
        is PhpIntegerKey -> key
        is PhpStringKey -> PhpStringKey(key.bytes.logicalPropertyName().compact())
    }

    /** Removes PHP's NUL-delimited private/protected property scope from VarDumper keys. */
    private fun PhpBytes.logicalPropertyName(): PhpBytes {
        if (size < 3 || byteAt(0) != 0.toByte()) {
            return this
        }

        val separator = indexOf(0.toByte(), 1)
        return if (separator > 1) {
            slice(separator + 1, size - separator - 1)
        } else {
            this
        }
    }

    private data class DataStub(
        val type: Int,
        val position: Long,
        val value: PhpValue?,
    )

    private companion object {
        const val VAR_DUMPER_NAMESPACE = "Symfony\\Component\\VarDumper\\"
        const val DATA_CLASS = "${VAR_DUMPER_NAMESPACE}Cloner\\Data"
        const val STUB_CLASS = "${VAR_DUMPER_NAMESPACE}Cloner\\Stub"
        const val TYPE_REFERENCE = 1
        const val TYPE_STRING = 2
        const val TYPE_ARRAY = 3
    }
}
