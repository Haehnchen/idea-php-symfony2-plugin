package fr.adrienbrault.idea.symfony2plugin.intentions.php

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.lang.LanguageImportStatements
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.jetbrains.php.lang.psi.PhpPsiElementFactory
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.GroupStatement
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.Parameter
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpPsiElement
import com.jetbrains.php.lang.psi.elements.Statement
import com.jetbrains.php.lang.psi.elements.UnaryExpression
import com.jetbrains.php.lang.psi.elements.Variable
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import icons.SymfonyIcons
import org.apache.commons.lang3.StringUtils
import java.util.Locale
import java.util.regex.Pattern
import javax.swing.Icon

/**
 * Intention action to migrate Symfony Command from execute() to __invoke() style (Symfony 7.3+)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class CommandToInvokableIntention : PsiElementBaseIntentionAction(), Iconable {
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java)
            ?: return

        val executeMethod = phpClass.findOwnMethodByName("execute")
            ?: return

        // Remove "extends Command" FIRST, before other PSI modifications
        // This must be done before replacing methods to avoid PSI invalidation issues
        if (removeCommandExtends(phpClass)) {
            // Remove parent::__construct() calls since we no longer extend Command
            removeParentConstructorCalls(phpClass)
        }

        // Get parameter names from the execute method
        val paramNames = extractParameterNames(executeMethod)

        // Collect arguments and options from configure() method
        val configureData = extractConfigureData(phpClass)

        // Migrate the execute method to __invoke by renaming and modifying parameters
        migrateExecuteToInvoke(project, executeMethod, configureData, paramNames)

        // Remove or update configure() method
        val configureMethod = phpClass.findOwnMethodByName("configure")
        if (configureMethod != null && configureData.canRemoveConfigure()) {
            configureMethod.delete()
        }

        // Optimize imports to remove unused import statements
        optimizeImports(phpClass.containingFile)
    }

    private fun removeCommandExtends(phpClass: PhpClass): Boolean {
        // Check if class extends Command
        if (!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false
        }

        // Use shared utility method to remove the extends clause
        return CodeUtil.removeExtendsClause(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")
    }

    /**
     * Removes parent::__construct() calls from the class constructor.
     * Since we're removing "extends Command", any parent constructor calls become invalid.
     */
    private fun removeParentConstructorCalls(phpClass: PhpClass) {
        // Find the constructor method
        val constructor = phpClass.constructor
            ?: return

        // Find the method body
        val body = PsiTreeUtil.findChildOfType(constructor, GroupStatement::class.java)
            ?: return

        // Find all parent::__construct() calls
        val methodReferences = PsiTreeUtil.findChildrenOfType(body, MethodReference::class.java)
        val parentConstructorCalls = mutableListOf<MethodReference>()

        for (methodRef in methodReferences) {
            // Check if this is a parent::__construct() call
            if ("__construct" == methodRef.name) {
                val classReference = methodRef.classReference
                if (classReference != null && "parent" == classReference.text) {
                    parentConstructorCalls.add(methodRef)
                }
            }
        }

        if (parentConstructorCalls.isEmpty()) {
            return
        }

        // Remove each parent::__construct() call statement
        for (parentCall in parentConstructorCalls) {
            // Navigate up to find the statement containing this call
            val statement = PsiTreeUtil.getParentOfType(parentCall, Statement::class.java)
            if (statement != null) {
                statement.delete()
            }
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

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        val phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java)
            ?: return false

        // Check if class extends Command
        if (!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false
        }

        // check if feature exists
        if (!PhpElementsUtil.hasClassOrInterface(project, "\\Symfony\\Component\\Console\\Command\\InvokableCommand")) {
            return false
        }

        // Check if execute method exists
        val executeMethod = phpClass.findOwnMethodByName("execute")
        if (executeMethod == null) {
            return false
        }

        // Check if __invoke doesn't already exist
        val invokeMethod = phpClass.findOwnMethodByName("__invoke")
        return invokeMethod == null
    }

    override fun getFamilyName(): String {
        return "Symfony: Migrate to invokable command"
    }

    override fun getText(): String {
        return familyName
    }

    override fun getIcon(flags: Int): Icon {
        return SymfonyIcons.Symfony
    }

    private fun extractParameterNames(executeMethod: Method): ParameterNames {
        val names = ParameterNames()

        val parameters: Array<Parameter> = executeMethod.parameters
        for (parameter in parameters) {
            val paramName = parameter.name

            // Get the actual type declaration text as written in source
            val typeText = parameter.typeDeclaration?.text ?: ""

            // Check the actual types to identify InputInterface and OutputInterface
            for (type in parameter.declaredType.types) {
                if (type == "\\Symfony\\Component\\Console\\Input\\InputInterface" || type == "\\Symfony\\Component\\Console\\Input\\OutputInterface") {
                    names.inputParam = paramName
                    names.inputType = typeText
                    break
                }
            }
        }

        return names
    }

    private fun extractConfigureData(phpClass: PhpClass): ConfigureData {
        val data = ConfigureData()

        val configureMethod = phpClass.findOwnMethodByName("configure")
            ?: return data

        // Extract arguments from $this->addArgument() calls
        val addArgumentCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "addArgument")
        for (methodRef in addArgumentCalls) {
            val argInfo = parseAddArgument(methodRef)
            if (argInfo != null) {
                data.arguments.add(argInfo)
            }
        }

        // Extract options from $this->addOption() calls
        val addOptionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "addOption")
        for (methodRef in addOptionCalls) {
            val optInfo = parseAddOption(methodRef)
            if (optInfo != null) {
                data.options.add(optInfo)
            }
        }

        // Check if configure has other important calls (setDescription, setHelp, etc.)
        val setDescriptionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "setDescription")
        val setHelpCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "setHelp")

        data.hasOtherConfigureCalls = setDescriptionCalls.isNotEmpty() || setHelpCalls.isNotEmpty()

        return data
    }

    private fun extractMethodParamName(methodRef: MethodReference): String? {
        val nameParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 0)
            ?: return null
        val name = PhpElementsUtil.getStringValue(nameParam)
        return if (StringUtils.isBlank(name)) null else name
    }

    private fun parseAddArgument(methodRef: MethodReference): ArgumentInfo? {
        // addArgument(name, mode, description, default)
        val info = ArgumentInfo()

        info.name = extractMethodParamName(methodRef)
        if (info.name == null) {
            return null
        }

        // Get mode (parameter 1) - InputArgument::REQUIRED, OPTIONAL, IS_ARRAY
        val modeParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 1)
        if (modeParam != null) {
            val modeStr = modeParam.text
            info.isOptional = modeStr.contains("OPTIONAL")
            info.isArray = modeStr.contains("IS_ARRAY")
        }

        // Get description (parameter 2)
        val descParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 2)
        if (descParam != null) {
            info.description = PhpElementsUtil.getStringValue(descParam)
        }

        // Get default value (parameter 3)
        val defaultParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 3)
        if (defaultParam != null) {
            info.defaultValue = defaultParam.text
            info.isOptional = true // If default value exists, it's optional
        }

        return info
    }

    private fun parseAddOption(methodRef: MethodReference): OptionInfo? {
        // addOption(name, shortcut, mode, description, default)
        val info = OptionInfo()

        info.name = extractMethodParamName(methodRef)
        if (info.name == null) {
            return null
        }

        // Get shortcut (parameter 1)
        val shortcutParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 1)
        if (shortcutParam != null) {
            info.shortcut = PhpElementsUtil.getStringValue(shortcutParam)
        }

        // Get mode (parameter 2) - InputOption::VALUE_NONE, VALUE_REQUIRED, VALUE_OPTIONAL, VALUE_IS_ARRAY
        val modeParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 2)
        if (modeParam != null) {
            val modeStr = modeParam.text
            info.isValueNone = modeStr.contains("VALUE_NONE")
            info.isArray = modeStr.contains("VALUE_IS_ARRAY") || modeStr.contains("IS_ARRAY")
            info.isRequired = modeStr.contains("VALUE_REQUIRED")
        }

        // Get description (parameter 3)
        val descParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 3)
        if (descParam != null) {
            info.description = PhpElementsUtil.getStringValue(descParam)
        }

        // Get default value (parameter 4)
        val defaultParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 4)
        if (defaultParam != null) {
            info.defaultValue = defaultParam.text
        }

        return info
    }

    private fun migrateExecuteToInvoke(project: Project, executeMethod: Method, configureData: ConfigureData, paramNames: ParameterNames) {
        // Check which parameters are actually used
        val usage = analyzeParameterUsage(executeMethod, paramNames)

        // Get the document for text-based manipulation
        val file = executeMethod.containingFile
        val document = PsiDocumentManager.getInstance(project).getDocument(file)

        if (document == null) {
            return
        }

        val psiDocManager = PsiDocumentManager.getInstance(project)

        // Add use statements for Argument and Option attributes if needed
        val phpClass = executeMethod.containingClass
        if (phpClass != null) {
            // Track which attributes we need
            val needsArgument = configureData.arguments.isNotEmpty()
            val needsOption = configureData.options.isNotEmpty()

            if (needsArgument) {
                PhpElementsUtil.insertUseIfNecessary(phpClass, "\\Symfony\\Component\\Console\\Attribute\\Argument")
            }
            if (needsOption) {
                PhpElementsUtil.insertUseIfNecessary(phpClass, "\\Symfony\\Component\\Console\\Attribute\\Option")
            }

            // Commit and unblock after adding use statements to allow further document modifications
            psiDocManager.doPostponedOperationsAndUnblockDocument(document)
        }

        // Build new parameter list
        val newParameters = buildNewParameterList(usage, paramNames, configureData)

        // Replace the entire method signature (from visibility modifier to the opening brace)
        replaceMethodSignature(executeMethod, newParameters, document)

        // Commit document changes to refresh PSI completely
        psiDocManager.commitDocument(document)
        psiDocManager.doPostponedOperationsAndUnblockDocument(document)

        // Get a fresh reference to the file and class for reformatting
        val freshFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        if (freshFile != null) {
            val freshPhpClass = PsiTreeUtil.findChildOfType(freshFile, PhpClass::class.java)
            if (freshPhpClass != null) {
                val invokeMethod = freshPhpClass.findOwnMethodByName("__invoke")
                if (invokeMethod != null) {
                    // Replace $input->getArgument() and $input->getOption() calls with direct variables
                    replaceInputMethodCallsWithVariables(invokeMethod, configureData, paramNames)

                    // Commit after replacements
                    psiDocManager.commitDocument(document)
                    psiDocManager.doPostponedOperationsAndUnblockDocument(document)

                    // Reformat the __invoke method
                    reformatMethod(project, invokeMethod)
                }
            }
        }

        // Final commit
        psiDocManager.doPostponedOperationsAndUnblockDocument(document)
    }

    /**
     * Replaces $input->getArgument('name') and $input->getOption('name') calls with direct variable access.
     * Also handles type casts like (int) $input->getArgument('name').
     * Removes redundant self-assignments like $var = $var.
     */
    private fun replaceInputMethodCallsWithVariables(invokeMethod: Method, configureData: ConfigureData, paramNames: ParameterNames) {
        val body = PsiTreeUtil.findChildOfType(invokeMethod, GroupStatement::class.java)
            ?: return

        // Build a map of argument/option names to their variable names
        val argumentVariableMap = mutableMapOf<String, String>()
        val optionVariableMap = mutableMapOf<String, String>()

        for (arg in configureData.arguments) {
            val name = arg.name!!
            val variableName = convertToValidPhpVariableName(name)
            argumentVariableMap[name] = variableName
        }

        for (opt in configureData.options) {
            val name = opt.name!!
            val variableName = convertToValidPhpVariableName(name)
            optionVariableMap[name] = variableName
        }

        // Find all method calls in the body
        val methodCalls = PsiTreeUtil.findChildrenOfType(body, MethodReference::class.java)
        val callsToReplace = mutableListOf<MethodReference>()

        for (methodCall in methodCalls) {
            val methodName = methodCall.name
                ?: continue

            // Check if this is $input->getArgument() or $input->getOption() on InputInterface
            if ("getArgument" == methodName) {
                if (PhpElementsUtil.isMethodReferenceInstanceOf(methodCall, "\\Symfony\\Component\\Console\\Input\\InputInterface", "getArgument")) {
                    callsToReplace.add(methodCall)
                }
            } else if ("getOption" == methodName) {
                if (PhpElementsUtil.isMethodReferenceInstanceOf(methodCall, "\\Symfony\\Component\\Console\\Input\\InputInterface", "getOption")) {
                    callsToReplace.add(methodCall)
                }
            }
        }

        // Replace each call
        for (methodCall in callsToReplace) {
            val methodName = methodCall.name
                ?: continue

            // Get the argument/option name from the first parameter
            val nameParam = PsiElementUtils.getMethodParameterPsiElementAt(methodCall, 0)
                ?: continue

            val paramName = PhpElementsUtil.getStringValue(nameParam)
            if (StringUtils.isBlank(paramName)) {
                continue
            }

            // Get the variable name from our map
            val variableName = if ("getArgument" == methodName) {
                argumentVariableMap[paramName]
            } else {
                optionVariableMap[paramName]
            }

            if (variableName == null) {
                continue
            }

            // Check if the method call is wrapped in a type cast
            val parent = methodCall.parent
            var elementToReplace: PsiElement = methodCall

            if (parent is UnaryExpression) {
                // Check if this is a cast expression by looking at the operator
                val operator = parent.operation
                if (operator != null) {
                    val operatorText = operator.text
                    // Common PHP type casts: (int), (string), (bool), (array), (object), (float), (double)
                    if (operatorText.matches(Regex("\\(\\s*(int|integer|string|bool|boolean|array|object|float|double|real)\\s*\\)"))) {
                        elementToReplace = parent
                    }
                }
            }

            // Create a new variable reference
            val newVariable: PhpPsiElement? = PhpPsiElementFactory.createFromText(
                invokeMethod.project,
                Variable::class.java,
                "\$$variableName",
            )

            if (newVariable != null) {
                elementToReplace.replace(newVariable)
            }
        }

        // Remove redundant self-assignments like $var = $var
        removeRedundantSelfAssignments(body)
    }

    /**
     * Removes redundant self-assignments like $var = $var.
     * These occur when we replace $input->getArgument('name') with $name,
     * and the original code had $name = $input->getArgument('name').
     */
    private fun removeRedundantSelfAssignments(body: GroupStatement) {
        val assignments = PsiTreeUtil.findChildrenOfType(body, AssignmentExpression::class.java)
        val statementsToRemove = mutableListOf<PsiElement>()

        for (assignment in assignments) {
            val variable = assignment.variable
            val value = assignment.value

            // Check if both left and right side are variables
            if (variable is Variable && value is Variable) {
                val leftVarName = variable.name
                val rightVarName = value.name

                // If they're the same variable, mark the statement for removal
                if (leftVarName != null && leftVarName == rightVarName) {
                    // Navigate up to find the statement containing this assignment
                    val statement = PsiTreeUtil.getParentOfType(assignment, Statement::class.java)
                    if (statement != null && !statementsToRemove.contains(statement)) {
                        statementsToRemove.add(statement)
                    }
                }
            }
        }

        // Remove all redundant statements
        for (statement in statementsToRemove) {
            statement.delete()
        }
    }

    private fun reformatMethod(project: Project, method: Method) {
        // Reformat only the method signature (parameters), not the entire body
        // Use CodeStyleManager directly to avoid creating extra undo events
        val containingFile = method.containingFile

        // Find the method body to determine where the signature ends
        val body = PsiTreeUtil.findChildOfType(method, GroupStatement::class.java)
            ?: return

        // Reformat from the start of the method to the start of the body (just the signature)
        val startOffset = method.textRange.startOffset
        val endOffset = body.textRange.startOffset

        val codeStyleManager = CodeStyleManager.getInstance(project)
        try {
            codeStyleManager.reformatRange(containingFile, startOffset, endOffset)
        } catch (_: IncorrectOperationException) {
            // Ignore formatting errors
        }
    }

    private fun replaceMethodSignature(method: Method, newParameters: List<String>, document: Document) {
        // Find the method signature in the source
        val methodText = method.text
        val methodStartOffset = method.textRange.startOffset

        // Find where the method body starts (the opening brace)
        val bodyStartIndex = methodText.indexOf('{')
        if (bodyStartIndex == -1) {
            return
        }

        // Build the new method signature
        val newSignature = StringBuilder("public function __invoke(")

        if (newParameters.isNotEmpty()) {
            newSignature.append("\n")
            newSignature.append(newParameters.joinToString(",\n"))
            newSignature.append("\n")
        }

        newSignature.append("): int ")

        // Replace everything from the start of the method to the opening brace
        val endOffset = methodStartOffset + bodyStartIndex
        document.replaceString(methodStartOffset, endOffset, newSignature.toString())
    }

    private fun buildNewParameterList(usage: ParameterUsage, paramNames: ParameterNames, configureData: ConfigureData): List<String> {
        // Separate required and optional parameters to ensure correct ordering
        val requiredParameters = mutableListOf<String>()
        val optionalParameters = mutableListOf<String>()

        // InputInterface and OutputInterface are always required parameters
        if (usage.inputUsed) {
            requiredParameters.add("${paramNames.inputType} \$${paramNames.inputParam}")
        }

        if (usage.outputUsed) {
            requiredParameters.add("${paramNames.outputType} \$${paramNames.outputParam}")
        }

        // Add arguments as parameters (sort by required/optional)
        for (arg in configureData.arguments) {
            val param = buildArgumentParameter(arg)
            if (arg.isOptional) {
                optionalParameters.add(param)
            } else {
                requiredParameters.add(param)
            }
        }

        // Add options as parameters (all options are optional)
        for (opt in configureData.options) {
            optionalParameters.add(buildOptionParameter(opt))
        }

        // Combine: required parameters first, then optional
        val parameters = mutableListOf<String>()
        parameters.addAll(requiredParameters)
        parameters.addAll(optionalParameters)

        return parameters
    }


    private fun analyzeParameterUsage(executeMethod: Method, paramNames: ParameterNames): ParameterUsage {
        val usage = ParameterUsage()

        val groupStatement = PsiTreeUtil.findChildOfType(executeMethod, GroupStatement::class.java)
            ?: return usage

        // Simply check if the input/output variables are used anywhere in the method body
        val variables = PsiTreeUtil.findChildrenOfType(groupStatement, Variable::class.java)

        for (variable in variables) {
            val varName = variable.name

            if (varName == paramNames.inputParam) {
                usage.inputUsed = true
            } else if (varName == paramNames.outputParam) {
                usage.outputUsed = true
            }
        }

        return usage
    }

    private fun buildArgumentParameter(arg: ArgumentInfo): String {
        val sb = StringBuilder()

        // Add #[Argument] attribute - use short name, use statement will be added separately
        sb.append("#[Argument(")

        val attributeParams = mutableListOf<String>()

        // Convert argument name to valid PHP variable name
        val name = arg.name!!
        val variableName = convertToValidPhpVariableName(name)

        // Add name parameter if the variable name differs from the original argument name
        if (variableName != name) {
            attributeParams.add("name: '${escapeString(name)}'")
        }

        // Add description if present
        if (StringUtils.isNotBlank(arg.description)) {
            attributeParams.add("description: '${escapeString(arg.description!!)}'")
        }

        sb.append(attributeParams.joinToString(", "))
        sb.append(")]")

        // Determine type - use nullable for optional parameters
        var type = if (arg.isArray) "array" else "string"
        if (arg.isOptional) {
            type = "?$type"
        }

        // Add parameter
        sb.append(" ").append(type).append(" $").append(variableName)

        // Add default value if optional
        if (arg.isOptional) {
            if (StringUtils.isNotBlank(arg.defaultValue)) {
                sb.append(" = ").append(arg.defaultValue)
            } else {
                sb.append(" = null")
            }
        }

        return sb.toString()
    }

    private fun buildOptionParameter(opt: OptionInfo): String {
        val sb = StringBuilder()

        // Add #[Option] attribute - use short name, use statement will be added separately
        sb.append("#[Option(")

        val attributeParams = mutableListOf<String>()

        // Convert option name to valid PHP variable name
        val name = opt.name!!
        val variableName = convertToValidPhpVariableName(name)

        // Add name parameter if the variable name differs from the original option name
        if (variableName != name) {
            attributeParams.add("name: '${escapeString(name)}'")
        }

        // Add shortcut if present
        if (StringUtils.isNotBlank(opt.shortcut)) {
            attributeParams.add("shortcut: '${opt.shortcut}'")
        }

        // Add description if present
        if (StringUtils.isNotBlank(opt.description)) {
            attributeParams.add("description: '${escapeString(opt.description!!)}'")
        }

        sb.append(attributeParams.joinToString(", "))
        sb.append(")]")

        // Determine type based on mode - use nullable for non-VALUE_NONE options
        val type = if (opt.isValueNone) {
            "bool"
        } else if (opt.isArray) {
            "?array"
        } else {
            "?string"
        }

        // Add parameter
        sb.append(" ").append(type).append(" $").append(variableName)

        // Add default value
        if (StringUtils.isNotBlank(opt.defaultValue)) {
            sb.append(" = ").append(opt.defaultValue)
        } else if (opt.isValueNone) {
            sb.append(" = false")
        } else {
            // For non-VALUE_NONE options without explicit default, use null
            sb.append(" = null")
        }

        return sb.toString()
    }



    private fun escapeString(str: String): String {
        return str.replace("'", "\\'")
    }

    /**
     * Converts an argument/option name to a valid PHP variable name.
     * PHP variable names must start with a letter or underscore, and can only contain
     * letters, numbers, and underscores.
     *
     * If the name contains invalid characters (like hyphens), it will be converted to camelCase.
     * For example: "user-name" -> "userName", "dry-run" -> "dryRun"
     *
     * @param name The original argument/option name
     * @return A valid PHP variable name
     */
    private fun convertToValidPhpVariableName(name: String): String {
        // Check if the name is already valid
        if (isValidPhpVariableName(name)) {
            return name
        }

        // Split by hyphens and other non-alphanumeric characters (except underscores)
        val parts = Pattern.compile("[^a-zA-Z0-9_]+").split(name)

        if (parts.isEmpty()) {
            // Fallback if the name is completely invalid
            return "arg"
        }

        val result = StringBuilder()

        for (i in parts.indices) {
            if (parts[i].isEmpty()) {
                continue
            }

            if (i == 0) {
                // First part: lowercase
                result.append(parts[i].lowercase(Locale.getDefault()))
            } else {
                // Subsequent parts: capitalize first letter (camelCase)
                result.append(parts[i][0].uppercaseChar())
                if (parts[i].length > 1) {
                    result.append(parts[i].substring(1).lowercase(Locale.getDefault()))
                }
            }
        }

        var finalName = result.toString()

        // Ensure it starts with a letter or underscore
        if (finalName.isNotEmpty() && !finalName[0].isLetter() && finalName[0] != '_') {
            finalName = "_$finalName"
        }

        return finalName.ifEmpty { "arg" }
    }

    /**
     * Checks if a string is a valid PHP variable name.
     *
     * @param name The name to check
     * @return true if the name is valid, false otherwise
     */
    private fun isValidPhpVariableName(name: String): Boolean {
        if (name.isEmpty()) {
            return false
        }

        // First character must be a letter or underscore
        val first = name[0]
        if (!first.isLetter() && first != '_') {
            return false
        }

        // Remaining characters must be letters, numbers, or underscores
        for (i in 1 until name.length) {
            val c = name[i]
            if (!c.isLetterOrDigit() && c != '_') {
                return false
            }
        }

        return true
    }

    // Data classes
    private class ParameterNames {
        var inputParam = "input"
        var outputParam = "output"
        var inputType = "InputInterface"
        var outputType = "OutputInterface"
    }

    private class ParameterUsage {
        var inputUsed = false
        var outputUsed = false
    }

    private class ConfigureData {
        val arguments = mutableListOf<ArgumentInfo>()
        val options = mutableListOf<OptionInfo>()
        var hasOtherConfigureCalls = false

        fun canRemoveConfigure(): Boolean {
            // Only remove configure if it only contains addArgument/addOption calls
            return !hasOtherConfigureCalls
        }
    }

    private class ArgumentInfo {
        var name: String? = null
        var description: String? = null
        var defaultValue: String? = null
        var isOptional = false
        var isArray = false
    }

    private class OptionInfo {
        var name: String? = null
        var shortcut: String? = null
        var description: String? = null
        var defaultValue: String? = null
        var isValueNone = false
        var isArray = false
        var isRequired = false
    }
}
