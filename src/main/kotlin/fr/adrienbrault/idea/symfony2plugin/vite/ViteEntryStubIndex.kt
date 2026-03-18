package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor

/**
 * File-based index for Vite entry points defined in vite.config.js / vite.config.ts.
 *
 * Key:   normalized target file path (e.g. "assets/app.ts") — leading "./" stripped
 * Value: entry name (e.g. "app")
 *
 * Allows efficient reverse lookup: given a JS/TS source file, find the Vite entry name(s)
 * that reference it, used by [ViteJavaScriptLineMarkerProvider].
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@JvmField
val VITE_ENTRY_STUB_INDEX_KEY: ID<String, String> = ID.create("fr.adrienbrault.idea.symfony2plugin.vite_entry_index")

class ViteEntryStubIndex : FileBasedIndexExtension<String, String>() {

    override fun getName(): ID<String, String> = VITE_ENTRY_STUB_INDEX_KEY

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getVersion(): Int = 1

    override fun dependsOnFileContent(): Boolean = true

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.name == "vite.config.js" || file.name == "vite.config.ts"
    }

    override fun getIndexer(): DataIndexer<String, String, FileContent> = DataIndexer { inputData ->
        val result = mutableMapOf<String, String>()

        val psiFile = inputData.psiFile
        if (psiFile is JSFile) {
            val entries = ViteConfigParser().parseEntries(psiFile)
            for (entry in entries) {
                val raw = entry.targetPath ?: continue
                val normalized = raw.removePrefix("./").removePrefix("/")
                if (normalized.isNotEmpty()) {
                    result[normalized] = entry.name
                }
            }
        }

        result
    }
}
