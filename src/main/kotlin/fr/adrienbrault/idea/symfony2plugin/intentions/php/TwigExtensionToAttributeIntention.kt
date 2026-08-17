package fr.adrienbrault.idea.symfony2plugin.intentions.php

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.lang.LanguageImportStatements
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.ClassReference
import com.jetbrains.php.lang.psi.elements.GroupStatement
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.NewExpression
import com.jetbrains.php.lang.psi.elements.PhpCallableMethod
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpReturn
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.Variable
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils
import icons.SymfonyIcons
import javax.swing.Icon

private const val ABSTRACT_EXTENSION_FQN = "\\Twig\\Extension\\AbstractExtension"
private const val TWIG_FILTER_FQN = "Twig\\Attribute\\AsTwigFilter"
private const val TWIG_FUNCTION_FQN = "Twig\\Attribute\\AsTwigFunction"
private const val TWIG_TEST_FQN = "Twig\\Attribute\\AsTwigTest"

/**
 * Intention action to migrate TwigExtension getFilters(), getFunctions(), and getTests()
 * methods to use PHP attributes (#[AsTwigFilter], #[AsTwigFunction], #[AsTwigTest]).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigExtensionToAttributeIntention : PsiElementBaseIntentionAction(), Iconable {
    override fun getFileModifierForPreview(target: PsiFile): FileModifier? {
        return null
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val phpClass = getTwigExtensionClass(project, psiElement)
            ?: return

        val transformations = findTransformations(phpClass)
        if (transformations.isEmpty()) {
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            for (transformation in transformations) {
                applyTransformation(project, transformation)
            }

            // After all transformations, check if we can remove the extends clause
            checkAndRemoveExtendsAbstractExtension(project, phpClass)
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        val phpClass = getTwigExtensionClass(project, psiElement)
            ?: return false

        return hasAnyTransformableMethods(phpClass)
    }

    /**
     * Fast check to determine if the class has any of the target methods with transformable content.
     * For performance, we do a lightweight check for non-empty arrays in isAvailable().
     * The full transformation analysis happens in invoke().
     */
    private fun hasAnyTransformableMethods(phpClass: PhpClass): Boolean {
        val getFiltersMethod = phpClass.findOwnMethodByName("getFilters")
        if (getFiltersMethod != null && hasNonEmptyReturnArray(getFiltersMethod)) {
            return true
        }

        val getFunctionsMethod = phpClass.findOwnMethodByName("getFunctions")
        if (getFunctionsMethod != null && hasNonEmptyReturnArray(getFunctionsMethod)) {
            return true
        }

        val getTestsMethod = phpClass.findOwnMethodByName("getTests")
        if (getTestsMethod != null && hasNonEmptyReturnArray(getTestsMethod)) {
            return true
        }

        return false
    }

    /**
     * Check if a method returns a non-empty array.
     * This is a lightweight check - we just look for NewExpression elements in the return array.
     */
    private fun hasNonEmptyReturnArray(method: Method): Boolean {
        val groupStatement = PsiTreeUtil.findChildOfType(method, GroupStatement::class.java)
            ?: return false

        for (child in groupStatement.children) {
            if (child is PhpReturn) {
                val returnValue = child.argument
                if (returnValue is ArrayCreationExpression) {
                    // Check if the array has any NewExpression elements
                    return PsiTreeUtil.findChildOfType(returnValue, NewExpression::class.java) != null
                }
            }
        }

        return false
    }

    override fun getFamilyName(): String {
        return "Migrate to TwigExtension attributes"
    }

    override fun getText(): String {
        return familyName
    }

    override fun getIcon(flags: Int): Icon {
        return SymfonyIcons.Symfony
    }

    private fun getTwigExtensionClass(project: Project, element: PsiElement): PhpClass? {
        val phpClass = PhpPsiUtil.getParentByCondition(element, true, PhpClass.INSTANCEOF, null) as? PhpClass
            ?: return null

        if (!PhpElementsUtil.isInstanceOf(phpClass, ABSTRACT_EXTENSION_FQN)) {
            return null
        }

        return phpClass
    }

    private fun findTransformations(phpClass: PhpClass): List<MethodTransformation> {
        val transformations = mutableListOf<MethodTransformation>()

        // Check for getFilters() method
        val getFiltersMethod = phpClass.findOwnMethodByName("getFilters")
        if (getFiltersMethod != null) {
            transformations.addAll(extractFilterTransformations(getFiltersMethod, phpClass))
        }

        // Check for getFunctions() method
        val getFunctionsMethod = phpClass.findOwnMethodByName("getFunctions")
        if (getFunctionsMethod != null) {
            transformations.addAll(extractFunctionTransformations(getFunctionsMethod, phpClass))
        }

        // Check for getTests() method
        val getTestsMethod = phpClass.findOwnMethodByName("getTests")
        if (getTestsMethod != null) {
            transformations.addAll(extractTestTransformations(getTestsMethod, phpClass))
        }

        return transformations
    }

    private fun extractFilterTransformations(getFiltersMethod: Method, phpClass: PhpClass): List<MethodTransformation> {
        return extractTransformations(getFiltersMethod, phpClass, TWIG_FILTER_FQN, "TwigFilter")
    }

    private fun extractFunctionTransformations(getFunctionsMethod: Method, phpClass: PhpClass): List<MethodTransformation> {
        return extractTransformations(getFunctionsMethod, phpClass, TWIG_FUNCTION_FQN, "TwigFunction")
    }

    private fun extractTestTransformations(getTestsMethod: Method, phpClass: PhpClass): List<MethodTransformation> {
        return extractTransformations(getTestsMethod, phpClass, TWIG_TEST_FQN, "TwigTest")
    }

    private fun extractTransformations(getMethod: Method, phpClass: PhpClass, attributeFqn: String, twigType: String): List<MethodTransformation> {
        val transformations = mutableListOf<MethodTransformation>()

        // Find the method body (GroupStatement)
        val groupStatement = PsiTreeUtil.findChildOfType(getMethod, GroupStatement::class.java)
            ?: return transformations

        // Look for array return values inside the method body
        for (child in groupStatement.children) {
            if (child is PhpReturn) {
                val returnValue = child.argument
                if (returnValue is ArrayCreationExpression) {
                    // Parse the array for TwigFilter/TwigFunction/TwigTest instances
                    extractFromArray(returnValue, transformations, twigType, attributeFqn, phpClass)
                }
            }
        }

        return transformations
    }

    private fun extractFromArray(arrayExpr: ArrayCreationExpression, transformations: MutableList<MethodTransformation>, twigType: String, attributeFqn: String, phpClass: PhpClass) {
        // Find all NewExpression elements in the array (handles both indexed and associative arrays)
        val newExpressions = PsiTreeUtil.findChildrenOfType(arrayExpr, NewExpression::class.java)

        for (newExpr in newExpressions) {
            // Check if this is a TwigFilter/TwigFunction/TwigTest instantiation
            val classReference: ClassReference = newExpr.classReference
                ?: continue

            val className = classReference.name
            if (twigType != className) {
                continue
            }

            // Extract parameters from the NewExpression
            val parameters = newExpr.parameters
            if (parameters.size < 2) {
                continue // Need at least name and callable
            }

            var name: String? = null
            var methodName: String? = null
            var options = ""

            // First parameter: name (string literal)
            val nameParameter = parameters[0]
            if (nameParameter is StringLiteralExpression) {
                name = nameParameter.contents
            }

            // Second parameter: callable array [$this, 'methodName'] or [SomeClass::class, 'methodName'] or first-class callable $this->method(...)
            val callableParameter = parameters[1]
            if (callableParameter is ArrayCreationExpression) {
                // Check if this is [$this, 'methodName'] - only migrate if it references $this
                val variables = PsiTreeUtil.findChildrenOfType(callableParameter, Variable::class.java)
                var referencesThis = false
                for (variable in variables) {
                    if ("this" == variable.name) {
                        referencesThis = true
                        break
                    }
                }
                // Only process if it references $this (not other classes)
                if (referencesThis) {
                    val stringLiterals = PsiTreeUtil.findChildrenOfType(callableParameter, StringLiteralExpression::class.java)
                    // The method name is the string literal that's not 'this'
                    for (literal in stringLiterals) {
                        val value = literal.contents
                        if (value.isNotEmpty() && value != "this") {
                            methodName = value
                            break
                        }
                    }
                }
            } else if (callableParameter is PhpCallableMethod) {
                // First-class callable syntax: $this->methodName(...)
                methodName = callableParameter.name
            }

            // Third parameter: options array (optional)
            val optionsParameter = parameters.getOrNull(2)
            if (optionsParameter is ArrayCreationExpression) {
                val optionElements = PsiTreeUtil.findChildrenOfType(optionsParameter, ArrayHashElement::class.java)
                val optionsBuilder = StringBuilder()

                for (hashElement in optionElements) {
                    val keyElement = hashElement.key
                    val valueElement = hashElement.value

                    if (keyElement is StringLiteralExpression && valueElement != null) {
                        var key = keyElement.contents
                        val value = valueElement.text

                        // Convert snake_case to camelCase for PHP attributes
                        key = StringUtils.camelize(key, true)

                        if (optionsBuilder.isNotEmpty()) {
                            optionsBuilder.append(", ")
                        }
                        optionsBuilder.append(key).append(": ").append(value)
                    }
                }

                options = optionsBuilder.toString()
            }

            // Validate that we have the required data
            if (name.isNullOrEmpty() || methodName.isNullOrEmpty()) {
                continue
            }

            // Find the target method in the class
            val targetMethod = phpClass.findOwnMethodByName(methodName)
                ?: continue // Method doesn't exist in the class

            // Find the element to delete from the array
            // The NewExpression is wrapped in PhpPsiElementImpl, which is the array element
            val elementToDelete = newExpr.parent

            transformations.add(
                MethodTransformation(
                    name,
                    methodName,
                    attributeFqn,
                    options,
                    targetMethod,
                    elementToDelete,
                )
            )
        }
    }

    private fun applyTransformation(project: Project, transformation: MethodTransformation) {
        val phpClass = transformation.method.containingClass
            ?: return

        // Add the attribute import using PhpElementsUtil
        val importedName = PhpElementsUtil.insertUseIfNecessary(phpClass, transformation.attributeFqn)
        if (importedName != null) {
            transformation.importedName = importedName
        }

        // Create and add the attribute to the method
        val attributeText = createAttributeText(transformation)
        addAttributeToMethod(project, transformation.method, attributeText)

        // Remove the corresponding entry from the getMethods/getFilters/getTests array
        removeFromArray(transformation)

        // If the array becomes empty, remove the entire getXXX method
        checkAndRemoveEmptyGetMethod(phpClass, transformation.attributeFqn)
    }

    private fun createAttributeText(transformation: MethodTransformation): String {
        val attributeName = transformation.importedName
            ?: transformation.attributeFqn.substring(transformation.attributeFqn.lastIndexOf("\\") + 1)

        return if (transformation.options.isEmpty()) {
            "#[${attributeName}('${transformation.name}')]"
        } else {
            "#[${attributeName}('${transformation.name}', ${transformation.options})]"
        }
    }

    private fun addAttributeToMethod(project: Project, method: Method, attributeText: String) {
        // Insert attribute text directly before the method
        val file = method.containingFile
        val document = PsiDocumentManager.getInstance(project).getDocument(file)
            ?: return

        val psiDocManager = PsiDocumentManager.getInstance(project)
        psiDocManager.doPostponedOperationsAndUnblockDocument(document)

        val methodStartOffset = method.textRange.startOffset
        val fullAttributeText = "$attributeText\n"

        document.insertString(methodStartOffset, fullAttributeText)
        psiDocManager.commitDocument(document)
        psiDocManager.doPostponedOperationsAndUnblockDocument(document)

        // Reformat the added attribute with proper indentation
        CodeUtil.reformatAddedAttribute(project, document, methodStartOffset)
    }

    private fun removeFromArray(transformation: MethodTransformation) {
        val arrayElement = transformation.arrayElement
            ?: return

        val prev = arrayElement.prevSibling
        val next = arrayElement.nextSibling

        // First delete the array element itself
        arrayElement.delete()

        // Then handle comma cleanup
        // After deleting the element, we need to check if there's a dangling comma
        if (prev != null && next != null) {
            // Element was in the middle, check for comma after or before
            if ("," == next.text) {
                next.delete()
            } else if (prev.text.endsWith(",")) {
                // Check if prev sibling is a comma or ends with comma
                if ("," == prev.text) {
                    prev.delete()
                }
            }
        } else if (prev != null && prev.text.trim().endsWith(",")) {
            // Last element, remove trailing comma from previous element
            if ("," == prev.text.trim()) {
                prev.delete()
            }
        } else if (next != null && "," == next.text.trim()) {
            // First element, remove leading comma
            next.delete()
        }
    }

    private fun checkAndRemoveEmptyGetMethod(phpClass: PhpClass, attributeFqn: String) {
        var methodName: String? = null
        if (attributeFqn == TWIG_FILTER_FQN) {
            methodName = "getFilters"
        } else if (attributeFqn == TWIG_FUNCTION_FQN) {
            methodName = "getFunctions"
        } else if (attributeFqn == TWIG_TEST_FQN) {
            methodName = "getTests"
        }

        if (methodName == null) {
            return
        }

        val getMethod = phpClass.findOwnMethodByName(methodName)
            ?: return

        // Check if the method returns an empty array
        // Need to look inside GroupStatement for the return statement
        val groupStatement = PsiTreeUtil.findChildOfType(getMethod, GroupStatement::class.java)
            ?: return

        var isEmpty = false
        for (child in groupStatement.children) {
            if (child is PhpReturn) {
                val returnValue = child.argument
                if (returnValue is ArrayCreationExpression) {
                    // Check if the array has no NewExpression elements
                    isEmpty = PsiTreeUtil.findChildOfType(returnValue, NewExpression::class.java) == null
                }
            }
        }

        if (isEmpty) {
            getMethod.delete()
        }
    }

    private fun checkAndRemoveExtendsAbstractExtension(project: Project, phpClass: PhpClass) {
        // Check if getFilters, getFunctions, and getTests methods are all removed
        if (phpClass.findOwnMethodByName("getFilters") != null ||
            phpClass.findOwnMethodByName("getFunctions") != null ||
            phpClass.findOwnMethodByName("getTests") != null
        ) {
            // Some get methods still exist, don't remove extends
            return
        }

        // Check if the class extends AbstractExtension
        val superClass = phpClass.superClass?.fqn
        if (ABSTRACT_EXTENSION_FQN != superClass && "AbstractExtension" != superClass) {
            // Doesn't extend AbstractExtension, nothing to do
            return
        }

        // Remove the extends clause using shared utility method
        if (CodeUtil.removeExtendsClause(phpClass, ABSTRACT_EXTENSION_FQN)) {
            // Optimize imports to remove unused AbstractExtension import
            optimizeImports(phpClass.containingFile)
        }
    }

    private fun optimizeImports(file: PsiFile) {
        val optimizers = LanguageImportStatements.INSTANCE.forFile(file)
        if (optimizers.isNotEmpty()) {
            for (optimizer in optimizers) {
                if (optimizer.supports(file)) {
                    val runnable = optimizer.processFile(file)
                    runnable.run()
                    break
                }
            }
        }
    }

    private class MethodTransformation(
        val name: String,
        val methodName: String,
        val attributeFqn: String,
        val options: String,
        val method: Method,
        val arrayElement: PsiElement?,
        var importedName: String? = null,
    )
}
