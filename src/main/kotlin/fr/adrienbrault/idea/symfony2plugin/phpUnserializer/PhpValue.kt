package fr.adrienbrault.idea.symfony2plugin.phpUnserializer

/** Lossless value kinds emitted by PHP's serialization grammar. */
sealed interface PhpValue

data object PhpNull : PhpValue {
    override fun toString(): String = "PhpNull"
}

data class PhpBoolean(val value: Boolean) : PhpValue {
    override fun toString(): String = "PhpBoolean"
}

data class PhpInteger(val value: Long) : PhpValue {
    override fun toString(): String = "PhpInteger"
}

data class PhpFloat(val value: Double) : PhpValue {
    override fun toString(): String = "PhpFloat"
}

data class PhpString(val bytes: PhpBytes) : PhpValue {
    override fun toString(): String = "PhpString(bytes=$bytes)"
}

sealed interface PhpArrayKey

data class PhpIntegerKey(val value: Long) : PhpArrayKey

data class PhpStringKey(val bytes: PhpBytes) : PhpArrayKey

data class PhpArrayEntry(
    val key: PhpArrayKey,
    val value: PhpValue,
) {
    override fun toString(): String = "PhpArrayEntry(key=$key, value=${value::class.simpleName})"
}

/** Ordered PHP array entries; integer and byte-string keys may be mixed. */
data class PhpArray(val entries: List<PhpArrayEntry>) : PhpValue {
    private val lookupIndex: Map<PhpArrayKey, PhpValue> by lazy {
        buildMap {
            this@PhpArray.entries.forEach { putIfAbsent(it.key, it.value) }
        }
    }

    operator fun get(key: Long): PhpValue? = lookupIndex[PhpIntegerKey(key)]

    operator fun get(key: String): PhpValue? = lookupIndex[PhpStringKey(PhpBytes.utf8(key))]

    operator fun get(key: PhpBytes): PhpValue? = lookupIndex[PhpStringKey(key)]

    override fun toString(): String = "PhpArray(entryCount=${entries.size})"
}

enum class PhpPropertyVisibility {
    PUBLIC,
    PROTECTED,
    PRIVATE,
}

data class PhpObjectProperty(
    val key: PhpArrayKey,
    val value: PhpValue,
    val visibility: PhpPropertyVisibility?,
    val declaringClass: PhpBytes?,
    val logicalName: PhpBytes?,
) {
    /** Raw property name, or `null` for an integer key produced by `__serialize()`. */
    val name: PhpBytes?
        get() = (key as? PhpStringKey)?.bytes

    override fun toString(): String =
        "PhpObjectProperty(key=$key, visibility=$visibility, declaringClass=$declaringClass, logicalName=$logicalName, value=${value::class.simpleName})"
}

/** An unloaded PHP object with its raw class name and ordered properties. */
data class PhpObject(
    val className: PhpBytes,
    val properties: List<PhpObjectProperty>,
) : PhpValue {
    private val lookupIndex: Map<PhpArrayKey, PhpValue> by lazy {
        buildMap {
            this@PhpObject.properties.forEach { property ->
                val key = when (property.key) {
                    is PhpIntegerKey -> property.key
                    is PhpStringKey -> property.logicalName?.let(::PhpStringKey)
                }
                if (key != null) {
                    putIfAbsent(key, property.value)
                }
            }
        }
    }

    operator fun get(logicalName: String): PhpValue? =
        lookupIndex[PhpStringKey(PhpBytes.utf8(logicalName))]

    operator fun get(key: Long): PhpValue? = lookupIndex[PhpIntegerKey(key)]

    override fun toString(): String =
        "PhpObject(className=$className, propertyCount=${properties.size})"
}

data class PhpEnum(
    val enumName: PhpBytes,
    val caseName: PhpBytes,
) : PhpValue {
    override fun toString(): String = "PhpEnum(enumName=$enumName, caseName=$caseName)"
}

enum class PhpReferenceKind {
    OBJECT,
    ALIAS,
}

/** A symbolic one-based reference into [PhpUnserializeResult]'s PHP traversal table. */
data class PhpReference(
    val id: Int,
    val kind: PhpReferenceKind,
) : PhpValue

/** A legacy `Serializable` (`C`) value whose payload is intentionally opaque. */
data class PhpCustomObject(
    val className: PhpBytes,
    val payload: PhpBytes,
) : PhpValue {
    override fun toString(): String =
        "PhpCustomObject(className=$className, payload=$payload)"
}

/** A root value plus the reference table needed to navigate aliases and cycles safely. */
class PhpUnserializeResult internal constructor(
    val root: PhpValue,
    private val references: List<PhpValue>,
    val parsedValueCount: Int,
) {
    val referenceCount: Int
        get() = references.size

    /**
     * Resolves a symbolic reference without recursively expanding the graph.
     *
     * Example: `result.resolve(array["self"]!!.requireReference())`
     */
    fun resolve(reference: PhpReference): PhpValue {
        var current = reference
        val visited = HashSet<Int>()

        while (true) {
            require(current.id in 1..references.size) { "Reference ID is outside this result" }
            require(visited.add(current.id)) { "Reference cycle in reference table" }

            val value = references[current.id - 1]
            if (value !is PhpReference) {
                return value
            }
            current = value
        }
    }

    override fun toString(): String =
        "PhpUnserializeResult(root=${root::class.simpleName}, referenceCount=$referenceCount, parsedValueCount=$parsedValueCount)"
}

fun PhpValue.requireArray(): PhpArray = this as? PhpArray
    ?: throw IllegalStateException("Expected PhpArray, got ${this::class.simpleName}")

fun PhpValue.requireObject(): PhpObject = this as? PhpObject
    ?: throw IllegalStateException("Expected PhpObject, got ${this::class.simpleName}")

fun PhpValue.requireReference(): PhpReference = this as? PhpReference
    ?: throw IllegalStateException("Expected PhpReference, got ${this::class.simpleName}")

fun PhpValue.utf8StringOrNull(): String? = (this as? PhpString)?.bytes?.utf8StringOrNull()

internal fun createPhpObjectProperty(key: PhpArrayKey, value: PhpValue): PhpObjectProperty {
    val name = (key as? PhpStringKey)?.bytes
        ?: return PhpObjectProperty(key, value, null, null, null)

    if (name.size >= 3 && name.byteAt(0) == 0.toByte()) {
        val separator = name.indexOf(0.toByte(), 1)
        if (separator > 1) {
            val scope = name.slice(1, separator - 1)
            val logicalName = name.slice(separator + 1, name.size - separator - 1)
            if (scope.size == 1 && scope.byteAt(0) == '*'.code.toByte()) {
                return PhpObjectProperty(
                    key,
                    value,
                    PhpPropertyVisibility.PROTECTED,
                    null,
                    logicalName,
                )
            }

            return PhpObjectProperty(
                key,
                value,
                PhpPropertyVisibility.PRIVATE,
                scope,
                logicalName,
            )
        }
    }

    return PhpObjectProperty(
        key,
        value,
        PhpPropertyVisibility.PUBLIC,
        null,
        name,
    )
}
