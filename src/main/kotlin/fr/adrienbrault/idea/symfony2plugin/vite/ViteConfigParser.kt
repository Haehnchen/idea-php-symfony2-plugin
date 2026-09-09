package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.lang.ecmascript6.psi.JSExportAssignment
import com.intellij.lang.javascript.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil

/**
 * Parses vite.config.js / vite.config.ts files and extracts entry points
 * from input, build.rollupOptions.input and build.rolldownOptions.input.
 *
 * ```javascript
 * const entries = { app: './assets/app.js' };
 * export default defineConfig({ input: { ...entries } });
 * ```
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object ViteConfigParser {

    fun parseEntries(file: PsiFile): List<ViteEntry> {
        if (file !is JSFile) return emptyList()
        return LocalConfig(file).parseEntries()
    }

    private class LocalConfig(private val file: JSFile) {
        private val variables = PsiTreeUtil.findChildrenOfType(file, JSVariable::class.java)
        private val activeExpressions = mutableSetOf<JSExpression>()
        private val activeObjects = mutableSetOf<JSObjectLiteralExpression>()

        fun parseEntries(): List<ViteEntry> {
            val entries = mutableListOf<ViteEntry>()
            for (export in PsiTreeUtil.findChildrenOfType(file, JSExportAssignment::class.java)) {
                for (config in objects(export.expression, 0, true)) {
                    val properties = properties(config, 0)
                    val builds = objects(properties["build"]?.value, 0)
                    val nestedInputs = builds.flatMap { build ->
                        val buildProperties = properties(build, 0)
                        val options = buildProperties["rolldownOptions"] ?: buildProperties["rollupOptions"]
                        objects(options?.value, 0).mapNotNull { properties(it, 0)["input"] }
                    }
                    val inputs = nestedInputs.ifEmpty { listOfNotNull(properties["input"]) }
                    for (input in inputs) {
                        for (obj in objects(input.value, 0)) {
                            for ((name, property) in properties(obj, 0)) {
                                val path = (property.value as? JSLiteralExpression)?.stringValue
                                entries.add(ViteEntry(name, path, file.virtualFile, property))
                            }
                        }
                    }
                }
            }
            return entries.distinctBy { it.psiElement }
        }

        // Only follow local syntax: this parser also runs inside a file-based indexer.
        private fun objects(expression: JSExpression?, depth: Int, config: Boolean = false): List<JSObjectLiteralExpression> {
            if (expression == null || depth > 32 || !activeExpressions.add(expression)) return emptyList()
            try {
                return when (expression) {
                    is JSObjectLiteralExpression -> listOf(expression)
                    is JSParenthesizedExpression -> objects(expression.innerExpression, depth + 1, config)
                    is JSReferenceExpression -> objects(localInitializer(expression), depth + 1, config)
                    is JSConditionalExpression -> objects(expression.thenBranch, depth + 1, config) +
                        objects(expression.elseBranch, depth + 1, config)
                    is JSCallExpression -> {
                        val method = expression.methodExpression as? JSReferenceExpression
                        if (config && method?.qualifier == null && method?.referenceName == "defineConfig") {
                            objects(expression.arguments.singleOrNull(), depth + 1, true)
                        } else emptyList()
                    }
                    is JSFunctionExpression -> if (config) {
                        val block = expression.block
                        if (block == null) {
                            objects(expression.children.filterIsInstance<JSExpression>().lastOrNull(), depth + 1, true)
                        } else {
                            val results = mutableListOf<JSObjectLiteralExpression>()
                            block.accept(object : PsiRecursiveElementWalkingVisitor() {
                                override fun visitElement(element: PsiElement) {
                                    if (element is JSFunction) return
                                    if (element is JSReturnStatement) {
                                        results.addAll(objects(element.expression, depth + 1, true))
                                        return
                                    }
                                    super.visitElement(element)
                                }
                            })
                            results
                        }
                    } else emptyList()
                    else -> emptyList()
                }
            } finally {
                activeExpressions.remove(expression)
            }
        }

        private fun properties(obj: JSObjectLiteralExpression, depth: Int): Map<String, JSProperty> {
            if (depth > 32 || !activeObjects.add(obj)) return emptyMap()
            val result = linkedMapOf<String, JSProperty>()
            for (child in obj.children) {
                when (child) {
                    is JSProperty -> child.name?.let { result[it] = child }
                    is JSSpreadExpression -> objects(child.expression, depth + 1).forEach {
                        result.putAll(properties(it, depth + 1))
                    }
                }
            }
            activeObjects.remove(obj)
            return result
        }

        private fun localInitializer(ref: JSReferenceExpression): JSExpression? {
            if (ref.qualifier != null) return null
            val name = ref.referenceName ?: return null
            var scope: PsiElement? = ref.parent
            while (scope != null) {
                if (scope is JSBlockStatement || scope is JSFunction || scope is JSFile) {
                    val variable = variables.firstOrNull { it.name == name && enclosingScope(it) == scope }
                    if (variable != null) return variable.initializer
                    if (scope is JSFunction && scope.parameterVariables.any { it.name == name }) return null
                }
                scope = scope.parent
            }
            return null
        }

        private fun enclosingScope(element: PsiElement): PsiElement? {
            var parent = element.parent
            while (parent != null && parent !is JSBlockStatement && parent !is JSFunction && parent !is JSFile) {
                parent = parent.parent
            }
            return parent
        }
    }
}
