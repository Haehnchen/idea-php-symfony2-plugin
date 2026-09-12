package fr.adrienbrault.idea.symfony2plugin.templating.documentation

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.model.Pointer
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageView
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil

class TwigTemplateDocumentationTarget internal constructor(
    private val context: TwigTemplateDocumentationContext,
    private val templateFile: VirtualFile,
) : DocumentationTarget {
    override fun createPointer(): Pointer<TwigTemplateDocumentationTarget> {
        val pointer = SmartPointerManager.createPointer(context.anchor)
        val selected = templateFile

        return Pointer {
            val anchor = pointer.element ?: return@Pointer null
            val restored = TwigTemplateDocumentationTargetProvider().resolveContext(anchor.containingFile, anchor.textOffset)
                ?: return@Pointer null

            if (TwigUtil.getTemplateFiles(anchor.project, restored.name).none { it == selected }) {
                return@Pointer null
            }

            TwigTemplateDocumentationTarget(restored, selected)
        }
    }

    private fun current(): TwigTemplateDocumentationContext? {
        val anchor = context.anchor
        if (!anchor.isValid) {
            return null
        }

        return TwigTemplateDocumentationTargetProvider().resolveContext(anchor.containingFile, anchor.textOffset)
    }

    override val navigatable: Navigatable?
        get() {
            val current = current() ?: return null

            val file = TwigUtil.getTemplateFiles(current.anchor.project, current.name)
                .singleOrNull { it == templateFile } ?: return null

            return PsiManager.getInstance(current.anchor.project).findFile(file)?.navigationElement as? Navigatable
        }

    override fun computePresentation(): TargetPresentation = TargetPresentation.builder(context.name)
        .icon(TwigFileType.INSTANCE.icon)
        .locationText(VfsExUtil.getRelativeProjectPath(context.anchor.project, templateFile) ?: templateFile.presentableUrl)
        .presentation()

    // Hints stay cheap; full index/PSI collection runs asynchronously under a read action.
    override fun computeDocumentationHint(): String? = current()?.name?.let(StringUtil::escapeXmlEntities)

    override fun computeDocumentation(): DocumentationResult {
        val pointer = createPointer()

        return DocumentationResult.asyncDocumentation {
            readAction {
                val target = pointer.dereference() ?: return@readAction null
                val current = target.current() ?: return@readAction null
                val data = collectTwigTemplateDocumentation(current)

                DocumentationResult.documentation(renderTwigTemplateDocumentation(data))
            }
        }
    }

    internal fun findUsagesContext(): DataContext? {
        val current = current() ?: return null
        if (!templateFile.isValid || templateFile !in TwigUtil.getTemplateFiles(current.anchor.project, current.name)) {
            return null
        }

        val template = PsiManager.getInstance(current.anchor.project).findFile(templateFile) ?: return null
        return SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, template.project)
            .add(CommonDataKeys.PSI_ELEMENT, template)
            .add(CommonDataKeys.PSI_FILE, template)
            .add(UsageView.USAGE_TARGETS_KEY, arrayOf<UsageTarget>(PsiElement2UsageTargetAdapter(template, true)))
            .build()
    }

}

internal fun collectTwigTemplateDocumentation(context: TwigTemplateDocumentationContext): TwigTemplateDocumentationData {
    val project = context.anchor.project
    val index = FileBasedIndex.getInstance()
    val scope = GlobalSearchScope.projectScope(project)
    val targets = TwigUtil.getTemplateFiles(project, context.name)

    val names = linkedSetOf(context.name)
    targets.forEach { names.addAll(TwigUtil.getTemplateNamesForFile(project, it)) }

    // The PHP usage index retains the spelling; the Twig indices normalize their keys.
    names.toList().forEach { name ->
        names.add(TwigUtil.normalizeTemplateName(name))
        if (name.startsWith("@") && !name.startsWith("@!")) {
            names.add("@!" + name.substring(1))
        }
    }

    val normalized = names.map(TwigUtil::normalizeTemplateName).toSet()
    val phpFiles = mutableSetOf<String>()
    val callers = mutableSetOf<String>()

    for (name in names) {
        ProgressManager.checkCanceled()
        index.processValues(PhpTwigTemplateUsageStubIndex.KEY, name, null, { file, usage ->
            ProgressManager.checkCanceled()
            phpFiles.add(file.url)
            callers.addAll(usage.scopes.map(::presentScope))
            true
        }, scope)
    }

    val usages = mutableMapOf<String, MutableSet<String>>()

    for (name in normalized) {
        ProgressManager.checkCanceled()
        index.processValues(TwigIncludeStubIndex.KEY, name, null, { file, include ->
            val type = when (include.type) {
                TYPE.INCLUDE -> "include"
                TYPE.INCLUDE_FUNCTION -> "include() / source()"
                TYPE.EMBED -> "embed"
                TYPE.IMPORT -> "import / from"
                TYPE.FROM -> "from"
                TYPE.FORM_THEME -> "form_theme"
                TYPE.BLOCK_FUNCTION -> "block"
                null -> "include"
            }

            usages.getOrPut(type) { mutableSetOf() }.add(file.url)
            true
        }, scope)

        index.getContainingFiles(TwigExtendsStubIndex.KEY, name, scope).forEach {
            usages.getOrPut("extends") { mutableSetOf() }.add(it.url)
        }
    }

    val parents = targets.associate { file ->
        ProgressManager.checkCanceled()
        file.url to index.getFileData(TwigExtendsStubIndex.KEY, file, project).keys.toSet()
    }

    return TwigTemplateDocumentationData(
        name = context.name,
        paths = targets.associate { it.url to (VfsExUtil.getRelativeProjectPath(project, it) ?: it.presentableUrl) },
        parents = parents,
        phpFiles = phpFiles.toSet(),
        callers = callers.toSet(),
        twigUsages = usages.mapValues { it.value.toSet() },
    )
}

private fun presentScope(scope: String): String {
    val name = scope.trimStart('\\')
    val separator = name.lastIndexOf('.')

    return if (separator < 0) name else name.substring(0, separator) + "::" + name.substring(separator + 1)
}


internal const val TEMPLATE_FIND_USAGES_LINK = "symfony-template-find-usages"

class TwigTemplateDocumentationLinkHandler : DocumentationLinkHandler {
    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        if (target !is TwigTemplateDocumentationTarget || url != TEMPLATE_FIND_USAGES_LINK) {
            return null
        }

        val pointer = target.createPointer()
        ApplicationManager.getApplication().invokeLater {
            val context = runReadActionBlocking {
                pointer.dereference()?.findUsagesContext()
            } ?: return@invokeLater

            val action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES)
            val event = AnActionEvent.createEvent(action, context, null, "TwigTemplateDocumentation", ActionUiKind.POPUP, null)
            ActionUtil.performAction(action, event)
        }

        return LinkResolveResult.resolvedTarget(target)
    }
}
