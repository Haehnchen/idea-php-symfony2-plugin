package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils

/**
 * File-based index for Twig templates that use Vite entry point functions:
 * - vite_entry_link_tags('entryName')
 * - vite_entry_script_tags('entryName')
 *
 * Key:   entry name (e.g. "app")
 * Value: empty string (file is the carrier)
 *
 * Allows the [ViteJavaScriptLineMarkerProvider] to navigate from a JS/TS entry
 * file to all Twig templates that reference it.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@JvmField
val VITE_TWIG_USAGE_STUB_INDEX_KEY: ID<String, String> = ID.create("fr.adrienbrault.idea.symfony2plugin.vite_twig_usage_index")

class ViteTwigUsageStubIndex : FileBasedIndexExtension<String, String>() {

    override fun getName(): ID<String, String> = VITE_TWIG_USAGE_STUB_INDEX_KEY
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    override fun getValueExternalizer(): DataExternalizer<String> = EnumeratorStringDescriptor.INSTANCE
    override fun getVersion(): Int = 1
    override fun dependsOnFileContent(): Boolean = true

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        FileBasedIndex.InputFilter { it.fileType == TwigFileType.INSTANCE }

    override fun getIndexer(): DataIndexer<String, String, FileContent> = DataIndexer { inputData ->
        val result = mutableMapOf<String, String>()
        val psiFile = inputData.psiFile

        if (psiFile is TwigFile) {
            val pattern = TwigPattern.getPrintBlockOrTagFunctionPattern(
                "vite_entry_link_tags", "vite_entry_script_tags"
            )
            psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (pattern.accepts(element)) {
                        val entryName = PsiElementUtils.trimQuote(element.text)
                        if (entryName.isNotBlank()) {
                            result[entryName] = ""
                        }
                    }
                    super.visitElement(element)
                }
            })
        }

        result
    }
}
