package fr.adrienbrault.idea.symfony2plugin.mcp

object McpCsvUtil {
    fun escape(value: String): String {
        return if (',' in value || '"' in value || '\n' in value) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
