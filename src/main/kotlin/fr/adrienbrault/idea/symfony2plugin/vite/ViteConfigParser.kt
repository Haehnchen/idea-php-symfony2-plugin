package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.lang.javascript.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor

/**
 * Parses vite.config.js / vite.config.ts files and extracts entry points
 * from the rollupOptions.input configuration.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ViteConfigParser {

    fun parseEntries(file: PsiFile): List<ViteEntry> {
        if (file !is JSFile) return emptyList()

        val entries = mutableListOf<ViteEntry>()

        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is JSProperty && element.name == "input") {
                    val value = element.value
                    if (value != null) {
                        extractInputEntries(value, file, entries)
                    }
                }
                super.visitElement(element)
            }
        })

        return entries
    }

    private fun extractInputEntries(
        inputValue: JSExpression,
        file: JSFile,
        entries: MutableList<ViteEntry>
    ) {
        when (inputValue) {
            is JSObjectLiteralExpression -> extractFromObject(inputValue, file, entries)
            is JSReferenceExpression -> {
                val resolved = resolveReference(inputValue, file)
                if (resolved != null) {
                    extractFromObject(resolved, file, entries)
                }
            }
        }
    }

    private fun extractFromObject(
        obj: JSObjectLiteralExpression,
        file: JSFile,
        entries: MutableList<ViteEntry>
    ) {
        for (child in obj.children) {
            when (child) {
                is JSProperty -> {
                    val name = child.name ?: continue
                    val value = child.value
                    val targetPath = when (value) {
                        is JSLiteralExpression -> value.stringValue
                        else -> null
                    }
                    entries.add(ViteEntry(name, targetPath, file.virtualFile, child))
                }
                is JSSpreadExpression -> {
                    val operand = child.expression
                    if (operand is JSReferenceExpression) {
                        val resolved = resolveReference(operand, file)
                        if (resolved != null) {
                            extractFromObject(resolved, file, entries)
                        }
                    }
                }
            }
        }
    }

    private fun resolveReference(ref: JSReferenceExpression, file: JSFile): JSObjectLiteralExpression? {
        val name = ref.referenceName ?: return null

        var result: JSObjectLiteralExpression? = null

        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (result != null) return
                if (element is JSVariable && element.name == name) {
                    val initializer = element.initializer
                    if (initializer is JSObjectLiteralExpression) {
                        result = initializer
                        return
                    }
                }
                super.visitElement(element)
            }
        })

        return result
    }
}
