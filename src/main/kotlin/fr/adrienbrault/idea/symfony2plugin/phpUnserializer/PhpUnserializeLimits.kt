package fr.adrienbrault.idea.symfony2plugin.phpUnserializer

/** Defensive bounds checked before input copying, allocation, or recursive descent. */
data class PhpUnserializeLimits(
    val maxInputBytes: Int = 5 * 1024 * 1024,
    val maxNestingDepth: Int = 256,
    val maxTotalValues: Int = 250_000,
    val maxContainerEntries: Int = 100_000,
    val maxPayloadBytes: Int = 2 * 1024 * 1024,
) {
    init {
        require(maxInputBytes >= 0) { "maxInputBytes must not be negative" }
        require(maxNestingDepth >= 0) { "maxNestingDepth must not be negative" }
        require(maxTotalValues >= 0) { "maxTotalValues must not be negative" }
        require(maxContainerEntries >= 0) { "maxContainerEntries must not be negative" }
        require(maxPayloadBytes >= 0) { "maxPayloadBytes must not be negative" }
    }
}
