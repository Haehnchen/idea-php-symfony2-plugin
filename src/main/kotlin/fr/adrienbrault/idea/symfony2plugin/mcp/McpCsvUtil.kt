package fr.adrienbrault.idea.symfony2plugin.mcp

object McpCsvUtil {
    fun escape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
