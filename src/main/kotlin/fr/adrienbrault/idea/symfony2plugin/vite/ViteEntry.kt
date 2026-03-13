package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

data class ViteEntry(
    val name: String,
    val targetPath: String?,
    val configFile: VirtualFile,
    val psiElement: PsiElement? = null
)
