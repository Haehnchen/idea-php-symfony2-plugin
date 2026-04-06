package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.util.io.FileUtil
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Matches project-relative MCP paths against Ant-style glob patterns.
 *
 * Matching is performed on normalized slash-separated paths only.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object McpGlobMatcher {
    /**
     * Matches a normalized project-relative path against a non-blank Ant-style glob.
     *
     * @param path project-relative path using "/" separators
     * @param glob Ant-style glob pattern using project-relative semantics
     */
    fun matches(path: String, glob: String): Boolean {
        val normalizedGlob = glob.trim()
        val normalizedPath = path.replace('\\', '/')

        return compilePattern(normalizedGlob)?.matcher(normalizedPath)?.matches() == true
    }

    private fun compilePattern(glob: String): Pattern? {
        val normalizedGlob = glob.replace('\\', '/')

        return try {
            Pattern.compile("^" + FileUtil.convertAntToRegexp(normalizedGlob, false) + "$")
        } catch (_: PatternSyntaxException) {
            null
        }
    }
}
