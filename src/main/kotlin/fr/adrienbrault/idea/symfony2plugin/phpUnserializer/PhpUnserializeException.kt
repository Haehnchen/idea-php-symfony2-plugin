package fr.adrienbrault.idea.symfony2plugin.phpUnserializer

/** A content-safe syntax or limit error at an exact byte offset. */
class PhpUnserializeException(
    val offset: Int,
    val tag: Char?,
    val reason: String,
) : IllegalArgumentException(buildMessage(offset, tag, reason)) {
    companion object {
        private fun buildMessage(offset: Int, tag: Char?, reason: String): String {
            val renderedTag = when {
                tag == null -> ""
                tag.code in 0x21..0x7e -> " (tag '$tag')"
                else -> " (tag 0x${tag.code.toString(16).uppercase().padStart(2, '0')})"
            }
            return "Invalid PHP serialized data at byte $offset$renderedTag: $reason"
        }
    }
}
