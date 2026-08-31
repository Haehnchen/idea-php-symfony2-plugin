package fr.adrienbrault.idea.symfony2plugin.markdown

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.jetbrains.twig.TwigFileType
import com.jetbrains.twig.parser.TwigParserDefinition

private val markdownTwigFileElementType = IFileElementType(
    "SYMFONY_MARKDOWN_TWIG_FILE",
    MarkdownTwigLanguage,
)

class MarkdownTwigParserDefinition : TwigParserDefinition() {
    override fun getFileNodeType(): IFileElementType = markdownTwigFileElementType

    override fun createFile(viewProvider: FileViewProvider): PsiFile = MarkdownTwigFile(viewProvider)
}

private class MarkdownTwigFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MarkdownTwigLanguage) {
    override fun getFileType(): FileType = TwigFileType.INSTANCE

    // PsiFileImpl delegates this call to the host Markdown view provider. Resolve against the injected Twig tree instead.
    override fun findElementAt(offset: Int): PsiElement? = this.node.findLeafElementAt(offset)?.psi

    override fun toString(): String = "Markdown Twig file"
}
