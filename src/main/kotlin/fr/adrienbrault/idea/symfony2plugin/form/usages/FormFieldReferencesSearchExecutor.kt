package fr.adrienbrault.idea.symfony2plugin.form.usages

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FormDataClassStubIndex
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * Finds `$builder->add('title')` for `$title`, `getTitle()` or `setTitle()`
 */
class FormFieldReferencesSearchExecutor : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>): Boolean =
        ReadAction.computeBlocking<Boolean, RuntimeException> {
            val target = parameters.elementToSearch as? PhpNamedElement ?: return@computeBlocking true

            processFormFieldUsages(target) { literal ->
                !PsiSearchScopeUtil.isInScope(parameters.effectiveSearchScope, literal) ||
                    consumer.process(FormFieldUsageReference(literal, target))
            }
        }
}

class FormFieldUsageReference(element: StringLiteralExpression, private val target: PhpNamedElement) :
    PsiReferenceBase<StringLiteralExpression>(element, element.valueRange) {
    override fun resolve(): PsiElement = target
    override fun getVariants(): Array<Any> = emptyArray()

    /**
     * ReferencesSearch is also used by Rename. This feature deliberately only adds usages.
     */
    override fun handleElementRename(newElementName: String): PsiElement = element
}

/**
 * Must be called under a read action. Candidates always come from the data-class index.
 */
internal fun processFormFieldUsages(target: PhpNamedElement, consumer: (StringLiteralExpression) -> Boolean): Boolean {
    if (!isSupportedFormTarget(target) || !Symfony2ProjectComponent.isEnabled(target.project)) return true

    val declaringClass = formTargetClass(target) ?: return true

    val project = target.project
    val manager = PsiManager.getInstance(project)
    val processed = HashSet<PsiElement>()

    /**
     * Finish index access before resolving PSI (which can itself access other indexes).
     */
    val candidates = linkedMapOf<com.intellij.openapi.vfs.VirtualFile, Set<String>>()
    val dataClassNames = linkedSetOf(declaringClass.fqn)
    PhpIndex.getInstance(project).processAllSubclasses(declaringClass.fqn) { subclass ->
        ProgressManager.checkCanceled()
        dataClassNames.add(subclass.fqn)
        true
    }

    for (fqn in dataClassNames) {
        ProgressManager.checkCanceled()
        FileBasedIndex.getInstance().processValues(
            FormDataClassStubIndex.KEY, "\\" + fqn.trimStart('\\'), null,
            { file, names -> candidates[file] = candidates[file].orEmpty() + names; true },
            GlobalSearchScope.projectScope(project)
        )
    }

    for ((file, names) in candidates) {
        ProgressManager.checkCanceled()
        val phpFile = manager.findFile(file) as? PhpFile ?: continue

        for (form in phpFile.topLevelDefs.values().filterIsInstance<PhpClass>().filter { it.fqn in names }) {
            val buildForm = form.findMethodByName("buildForm") ?: continue

            val dataClass = FormOptionsUtil.getFormPhpClassFromContext(form.firstChild) ?: continue
            if (dataClass.fqn !in dataClassNames || !hasUnambiguousDataClass(form, dataClass)) continue

            val roots = rootBuilderParameters(buildForm)
            for (call in FormUtil.getFormBuilderTypes(buildForm)) {
                ProgressManager.checkCanceled()
                val literal = call.parameters.firstOrNull() as? StringLiteralExpression ?: continue
                if (!isStaticLiteral(literal) || !hasDefaultMapping(call, literal.contents)) continue

                val method = PsiTreeUtil.getParentOfType(call, Function::class.java) as? Method ?: continue
                if (!isRootBuilder(call.classReference, method, roots[method].orEmpty())) continue

                if (!resolvesToFormTarget(literal, dataClass, target)) continue

                if (processed.add(literal) && !consumer(literal)) return false
            }
        }
    }

    return true
}

/**
 * The index may contain multiple defaults for one form; do not silently choose the first.
 */
private fun hasUnambiguousDataClass(form: PhpClass, dataClass: PhpClass): Boolean {
    for (methodName in FormOptionsUtil.FORM_OPTION_METHODS) {
        val method = form.findMethodByName(methodName) ?: continue

        for (call in PhpElementsUtil.collectMethodReferencesInsideControlFlow(method, "setDefault", "setDefaults")) {
            val value = when (call.name) {
                "setDefault" -> {
                    val key = call.parameters.firstOrNull() as? StringLiteralExpression ?: return false
                    if (key.contents != "data_class") continue

                    call.parameters.getOrNull(1) ?: return false
                }
                else -> {
                    val options = call.parameters.firstOrNull() as? ArrayCreationExpression ?: return false

                    PhpElementsUtil.getArrayValue(options, "data_class") ?: continue
                }
            }

            val fqn = PhpElementsUtil.getStringValue(value) ?: return false
            if (!PhpLangUtil.equalsClassNames(fqn.trimStart('\\'), dataClass.fqn.trimStart('\\'))) return false
        }
    }

    return true
}

private fun isStaticLiteral(literal: StringLiteralExpression): Boolean =
    PsiTreeUtil.findChildOfType(literal, Variable::class.java) == null && literal.contents.isNotEmpty()

private fun hasDefaultMapping(call: MethodReference, name: String): Boolean {
    val options = call.parameters.getOrNull(2) ?: return true
    if (options !is ArrayCreationExpression) return false

    for (entry in options.hashElements) {
        val key = entry.key as? StringLiteralExpression ?: return false
        if (!isStaticLiteral(key)) return false

        val value = entry.value
        when (key.contents) {
            "mapped" -> if (!value?.text.equals("true", ignoreCase = true)) return false
            "property_path" -> if (value !is StringLiteralExpression || !isStaticLiteral(value) || value.contents != name) return false
        }
    }

    /**
     * Reject unpacked/dynamic entries too: their options can override mapping.
     */
    return options.children.filterIsInstance<PhpPsiElement>().all { it is ArrayHashElement }
}

/**
 * Mirror FormUtil's one-hop helper traversal, retaining which argument carries the root builder.
 */
private fun rootBuilderParameters(buildForm: Method): Map<Method, Set<String>> {
    val roots = buildForm.parameters.filter { parameter ->
        parameter.type.types.any { it.trimStart('\\').equals("Symfony\\Component\\Form\\FormBuilderInterface", true) }
    }.map { it.name }.toSet()

    val result = mutableMapOf(buildForm to roots)
    val rejected = mutableMapOf<Method, MutableSet<String>>()

    for (call in PsiTreeUtil.findChildrenOfType(buildForm, MethodReference::class.java)) {
        ProgressManager.checkCanceled()
        if (PsiTreeUtil.getParentOfType(call, Function::class.java) != buildForm) continue

        val helper = call.resolve() as? Method ?: continue
        if (helper == buildForm) continue

        for ((index, parameter) in helper.parameters.withIndex()) {
            val argument = call.parameters.getOrNull(index)
            if (isRootBuilder(argument, buildForm, roots)) {
                result[helper] = result[helper].orEmpty() + parameter.name
            } else {
                rejected.getOrPut(helper) { hashSetOf() }.add(parameter.name)
            }
        }
    }

    return result.mapValues { (method, names) -> names - rejected[method].orEmpty() }
}

private fun isRootBuilder(expression: PsiElement?, method: Method, parameters: Set<String>): Boolean {
    /**
     * add() returns the same builder; create()/get() return a child builder.
     */
    if (expression is MethodReference) {
        return expression.name == "add" && isRootBuilder(expression.classReference, method, parameters)
    }

    if (expression !is Variable || expression.name !in parameters) return false

    /**
     * Be conservative when a parameter has been reassigned, even on another control-flow branch.
     */
    return PsiTreeUtil.findChildrenOfType(method, AssignmentExpression::class.java).none {
        (it.variable as? Variable)?.name == expression.name
    }
}
