package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.LanguageImportStatements;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import icons.SymfonyIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Intention action to migrate Symfony Command from execute() to __invoke() style (Symfony 7.3+)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CommandToInvokableIntention extends PsiElementBaseIntentionAction implements Iconable {
    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return;
        }

        Method executeMethod = phpClass.findOwnMethodByName("execute");
        if (executeMethod == null) {
            return;
        }

        // Remove "extends Command" FIRST, before other PSI modifications
        // This must be done before replacing methods to avoid PSI invalidation issues
        if (removeCommandExtends(phpClass)) {
            // Remove parent::__construct() calls since we no longer extend Command
            removeParentConstructorCalls(phpClass);
        }

        // Get parameter names from the execute method
        ParameterNames paramNames = extractParameterNames(executeMethod);

        // Collect arguments and options from configure() method
        ConfigureData configureData = extractConfigureData(phpClass);

        // Migrate the execute method to __invoke by renaming and modifying parameters
        migrateExecuteToInvoke(project, executeMethod, configureData, paramNames);

        // Remove or update configure() method
        Method configureMethod = phpClass.findOwnMethodByName("configure");
        if (configureMethod != null && configureData.canRemoveConfigure()) {
            configureMethod.delete();
        }

        // Optimize imports to remove unused import statements
        optimizeImports(phpClass.getContainingFile());
    }

    private boolean removeCommandExtends(@NotNull PhpClass phpClass) {
        // Check if class extends Command
        if (!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false;
        }

        // Use shared utility method to remove the extends clause
        return CodeUtil.removeExtendsClause(phpClass, "\\Symfony\\Component\\Console\\Command\\Command");
    }

    /**
     * Removes parent::__construct() calls from the class constructor.
     * Since we're removing "extends Command", any parent constructor calls become invalid.
     */
    private void removeParentConstructorCalls(@NotNull PhpClass phpClass) {
        // Find the constructor method
        Method constructor = phpClass.getConstructor();
        if (constructor == null) {
            return;
        }

        // Find the method body
        GroupStatement body = PsiTreeUtil.findChildOfType(constructor, GroupStatement.class);
        if (body == null) {
            return;
        }

        // Find all parent::__construct() calls
        Collection<MethodReference> methodReferences = PsiTreeUtil.findChildrenOfType(body, MethodReference.class);
        List<MethodReference> parentConstructorCalls = new ArrayList<>();

        for (MethodReference methodRef : methodReferences) {
            // Check if this is a parent::__construct() call
            if ("__construct".equals(methodRef.getName())) {
                PsiElement classReference = methodRef.getClassReference();
                if (classReference != null && "parent".equals(classReference.getText())) {
                    parentConstructorCalls.add(methodRef);
                }
            }
        }

        if (parentConstructorCalls.isEmpty()) {
            return;
        }

        // Remove each parent::__construct() call statement
        for (MethodReference parentCall : parentConstructorCalls) {
            // Navigate up to find the statement containing this call
            PsiElement statement = PsiTreeUtil.getParentOfType(parentCall, com.jetbrains.php.lang.psi.elements.Statement.class);
            if (statement != null) {
                statement.delete();
            }
        }
    }

    private void optimizeImports(@NotNull PsiFile file) {
        var optimizers = LanguageImportStatements.INSTANCE.forFile(file);
        if (!optimizers.isEmpty()) {
            for (var optimizer : optimizers) {
                if (optimizer.supports(file)) {
                    Runnable runnable = optimizer.processFile(file);
                    runnable.run();
                    break;
                }
            }
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return false;
        }

        // Check if class extends Command
        if (!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false;
        }

        // check if feature exists
        if (!PhpElementsUtil.hasClassOrInterface(project, "\\Symfony\\Component\\Console\\Command\\InvokableCommand")) {
            return false;
        }

        // Check if execute method exists
        Method executeMethod = phpClass.findOwnMethodByName("execute");
        if (executeMethod == null) {
            return false;
        }

        // Check if __invoke doesn't already exist
        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        return invokeMethod == null;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony: Migrate to invokable command";
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public Icon getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }

    @NotNull
    private ParameterNames extractParameterNames(@NotNull Method executeMethod) {
        ParameterNames names = new ParameterNames();

        Parameter[] parameters = executeMethod.getParameters();
        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();

            // Get the actual type declaration text as written in source
            String typeText = parameter.getTypeDeclaration() != null
                ? parameter.getTypeDeclaration().getText()
                : "";

            // Check the actual types to identify InputInterface and OutputInterface
            for (String type : parameter.getDeclaredType().getTypes()) {
                if (type.equals("\\Symfony\\Component\\Console\\Input\\InputInterface") || type.equals("\\Symfony\\Component\\Console\\Input\\OutputInterface")) {
                    names.inputParam = paramName;
                    names.inputType = typeText;
                    break;
                }
            }
        }

        return names;
    }

    @NotNull
    private ConfigureData extractConfigureData(@NotNull PhpClass phpClass) {
        ConfigureData data = new ConfigureData();

        Method configureMethod = phpClass.findOwnMethodByName("configure");
        if (configureMethod == null) {
            return data;
        }

        // Extract arguments from $this->addArgument() calls
        Collection<MethodReference> addArgumentCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "addArgument");
        for (MethodReference methodRef : addArgumentCalls) {
            ArgumentInfo argInfo = parseAddArgument(methodRef);
            if (argInfo != null) {
                data.arguments.add(argInfo);
            }
        }

        // Extract options from $this->addOption() calls
        Collection<MethodReference> addOptionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "addOption");
        for (MethodReference methodRef : addOptionCalls) {
            OptionInfo optInfo = parseAddOption(methodRef);
            if (optInfo != null) {
                data.options.add(optInfo);
            }
        }

        // Check if configure has other important calls (setDescription, setHelp, etc.)
        Collection<MethodReference> setDescriptionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "setDescription");
        Collection<MethodReference> setHelpCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "setHelp");

        data.hasOtherConfigureCalls = !setDescriptionCalls.isEmpty() || !setHelpCalls.isEmpty();

        return data;
    }

    @Nullable
    private ArgumentInfo parseAddArgument(@NotNull MethodReference methodRef) {
        // addArgument(name, mode, description, default)
        ArgumentInfo info = new ArgumentInfo();

        // Get name (parameter 0)
        PsiElement nameParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 0);
        if (nameParam != null) {
            info.name = PhpElementsUtil.getStringValue(nameParam);
        }

        if (StringUtils.isBlank(info.name)) {
            return null;
        }

        // Get mode (parameter 1) - InputArgument::REQUIRED, OPTIONAL, IS_ARRAY
        PsiElement modeParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 1);
        if (modeParam != null) {
            String modeStr = modeParam.getText();
            info.isOptional = modeStr.contains("OPTIONAL");
            info.isArray = modeStr.contains("IS_ARRAY");
        }

        // Get description (parameter 2)
        PsiElement descParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 2);
        if (descParam != null) {
            info.description = PhpElementsUtil.getStringValue(descParam);
        }

        // Get default value (parameter 3)
        PsiElement defaultParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 3);
        if (defaultParam != null) {
            info.defaultValue = defaultParam.getText();
            info.isOptional = true; // If default value exists, it's optional
        }

        return info;
    }

    @Nullable
    private OptionInfo parseAddOption(@NotNull MethodReference methodRef) {
        // addOption(name, shortcut, mode, description, default)
        OptionInfo info = new OptionInfo();

        // Get name (parameter 0)
        PsiElement nameParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 0);
        if (nameParam != null) {
            info.name = PhpElementsUtil.getStringValue(nameParam);
        }

        if (StringUtils.isBlank(info.name)) {
            return null;
        }

        // Get shortcut (parameter 1)
        PsiElement shortcutParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 1);
        if (shortcutParam != null) {
            info.shortcut = PhpElementsUtil.getStringValue(shortcutParam);
        }

        // Get mode (parameter 2) - InputOption::VALUE_NONE, VALUE_REQUIRED, VALUE_OPTIONAL, VALUE_IS_ARRAY
        PsiElement modeParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 2);
        if (modeParam != null) {
            String modeStr = modeParam.getText();
            info.isValueNone = modeStr.contains("VALUE_NONE");
            info.isArray = modeStr.contains("VALUE_IS_ARRAY") || modeStr.contains("IS_ARRAY");
            info.isRequired = modeStr.contains("VALUE_REQUIRED");
        }

        // Get description (parameter 3)
        PsiElement descParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 3);
        if (descParam != null) {
            info.description = PhpElementsUtil.getStringValue(descParam);
        }

        // Get default value (parameter 4)
        PsiElement defaultParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 4);
        if (defaultParam != null) {
            info.defaultValue = defaultParam.getText();
        }

        return info;
    }

    private void migrateExecuteToInvoke(@NotNull Project project, @NotNull Method executeMethod, @NotNull ConfigureData configureData, @NotNull ParameterNames paramNames) {
        // Check which parameters are actually used
        ParameterUsage usage = analyzeParameterUsage(executeMethod, paramNames);

        // Get the document for text-based manipulation
        PsiFile file = executeMethod.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);

        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

        // Add use statements for Argument and Option attributes if needed
        PhpClass phpClass = executeMethod.getContainingClass();
        if (phpClass != null) {
            // Track which attributes we need
            boolean needsArgument = !configureData.arguments.isEmpty();
            boolean needsOption = !configureData.options.isEmpty();

            if (needsArgument) {
                PhpElementsUtil.insertUseIfNecessary(phpClass, "\\Symfony\\Component\\Console\\Attribute\\Argument");
            }
            if (needsOption) {
                PhpElementsUtil.insertUseIfNecessary(phpClass, "\\Symfony\\Component\\Console\\Attribute\\Option");
            }

            // Commit and unblock after adding use statements to allow further document modifications
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);
        }

        // Build new parameter list
        List<String> newParameters = buildNewParameterList(usage, paramNames, configureData);

        // Replace the entire method signature (from visibility modifier to the opening brace)
        replaceMethodSignature(executeMethod, newParameters, document);

        // Commit document changes to refresh PSI completely
        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        // Get a fresh reference to the file and class for reformatting
        PsiFile freshFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (freshFile != null) {
            PhpClass freshPhpClass = PsiTreeUtil.findChildOfType(freshFile, PhpClass.class);
            if (freshPhpClass != null) {
                Method invokeMethod = freshPhpClass.findOwnMethodByName("__invoke");
                if (invokeMethod != null) {
                    // Replace $input->getArgument() and $input->getOption() calls with direct variables
                    replaceInputMethodCallsWithVariables(invokeMethod, configureData, paramNames);

                    // Commit after replacements
                    psiDocManager.commitDocument(document);
                    psiDocManager.doPostponedOperationsAndUnblockDocument(document);

                    // Reformat the __invoke method
                    reformatMethod(project, invokeMethod);
                }
            }
        }

        // Final commit
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);
    }

    /**
     * Replaces $input->getArgument('name') and $input->getOption('name') calls with direct variable access.
     * Also handles type casts like (int) $input->getArgument('name').
     * Removes redundant self-assignments like $var = $var.
     */
    private void replaceInputMethodCallsWithVariables(@NotNull Method invokeMethod, @NotNull ConfigureData configureData, @NotNull ParameterNames paramNames) {
        GroupStatement body = PsiTreeUtil.findChildOfType(invokeMethod, GroupStatement.class);
        if (body == null) {
            return;
        }

        // Build a map of argument/option names to their variable names
        java.util.Map<String, String> argumentVariableMap = new java.util.HashMap<>();
        java.util.Map<String, String> optionVariableMap = new java.util.HashMap<>();

        for (ArgumentInfo arg : configureData.arguments) {
            String variableName = convertToValidPhpVariableName(arg.name);
            argumentVariableMap.put(arg.name, variableName);
        }

        for (OptionInfo opt : configureData.options) {
            String variableName = convertToValidPhpVariableName(opt.name);
            optionVariableMap.put(opt.name, variableName);
        }

        // Find all method calls in the body
        Collection<MethodReference> methodCalls = PsiTreeUtil.findChildrenOfType(body, MethodReference.class);
        List<MethodReference> callsToReplace = new ArrayList<>();

        for (MethodReference methodCall : methodCalls) {
            String methodName = methodCall.getName();
            if (methodName == null) {
                continue;
            }

            // Check if this is $input->getArgument() or $input->getOption() on InputInterface
            if ("getArgument".equals(methodName)) {
                if (PhpElementsUtil.isMethodReferenceInstanceOf(methodCall, "\\Symfony\\Component\\Console\\Input\\InputInterface", "getArgument")) {
                    callsToReplace.add(methodCall);
                }
            } else if ("getOption".equals(methodName)) {
                if (PhpElementsUtil.isMethodReferenceInstanceOf(methodCall, "\\Symfony\\Component\\Console\\Input\\InputInterface", "getOption")) {
                    callsToReplace.add(methodCall);
                }
            }
        }

        // Replace each call
        for (MethodReference methodCall : callsToReplace) {
            String methodName = methodCall.getName();
            if (methodName == null) {
                continue;
            }

            // Get the argument/option name from the first parameter
            PsiElement nameParam = PsiElementUtils.getMethodParameterPsiElementAt(methodCall, 0);
            if (nameParam == null) {
                continue;
            }

            String paramName = PhpElementsUtil.getStringValue(nameParam);
            if (StringUtils.isBlank(paramName)) {
                continue;
            }

            // Get the variable name from our map
            String variableName;
            if ("getArgument".equals(methodName)) {
                variableName = argumentVariableMap.get(paramName);
            } else {
                variableName = optionVariableMap.get(paramName);
            }

            if (variableName == null) {
                continue;
            }

            // Check if the method call is wrapped in a type cast
            PsiElement parent = methodCall.getParent();
            PsiElement elementToReplace = methodCall;

            if (parent instanceof UnaryExpression) {
                UnaryExpression unaryExpr = (UnaryExpression) parent;
                // Check if this is a cast expression by looking at the operator
                PsiElement operator = unaryExpr.getOperation();
                if (operator != null) {
                    String operatorText = operator.getText();
                    // Common PHP type casts: (int), (string), (bool), (array), (object), (float), (double)
                    if (operatorText.matches("\\(\\s*(int|integer|string|bool|boolean|array|object|float|double|real)\\s*\\)")) {
                        elementToReplace = unaryExpr;
                    }
                }
            }

            // Create a new variable reference
            PhpPsiElement newVariable = PhpPsiElementFactory.createFromText(
                invokeMethod.getProject(),
                Variable.class,
                "$" + variableName
            );

            if (newVariable != null) {
                elementToReplace.replace(newVariable);
            }
        }

        // Remove redundant self-assignments like $var = $var
        removeRedundantSelfAssignments(body);
    }

    /**
     * Removes redundant self-assignments like $var = $var.
     * These occur when we replace $input->getArgument('name') with $name,
     * and the original code had $name = $input->getArgument('name').
     */
    private void removeRedundantSelfAssignments(@NotNull GroupStatement body) {
        Collection<AssignmentExpression> assignments = PsiTreeUtil.findChildrenOfType(body, AssignmentExpression.class);
        List<PsiElement> statementsToRemove = new ArrayList<>();

        for (AssignmentExpression assignment : assignments) {
            PhpPsiElement variable = assignment.getVariable();
            PhpPsiElement value = assignment.getValue();

            // Check if both left and right side are variables
            if (variable instanceof Variable && value instanceof Variable) {
                String leftVarName = ((Variable) variable).getName();
                String rightVarName = ((Variable) value).getName();

                // If they're the same variable, mark the statement for removal
                if (leftVarName != null && leftVarName.equals(rightVarName)) {
                    // Navigate up to find the statement containing this assignment
                    PsiElement statement = PsiTreeUtil.getParentOfType(assignment, com.jetbrains.php.lang.psi.elements.Statement.class);
                    if (statement != null && !statementsToRemove.contains(statement)) {
                        statementsToRemove.add(statement);
                    }
                }
            }
        }

        // Remove all redundant statements
        for (PsiElement statement : statementsToRemove) {
            statement.delete();
        }
    }

    private void reformatMethod(@NotNull Project project, @NotNull Method method) {
        // Reformat only the method signature (parameters), not the entire body
        // Use CodeStyleManager directly to avoid creating extra undo events
        PsiFile containingFile = method.getContainingFile();

        // Find the method body to determine where the signature ends
        GroupStatement body = PsiTreeUtil.findChildOfType(method, GroupStatement.class);
        if (body == null) {
            return;
        }

        // Reformat from the start of the method to the start of the body (just the signature)
        int startOffset = method.getTextRange().getStartOffset();
        int endOffset = body.getTextRange().getStartOffset();

        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        try {
            codeStyleManager.reformatRange(containingFile, startOffset, endOffset);
        } catch (com.intellij.util.IncorrectOperationException e) {
            // Ignore formatting errors
        }
    }

    private void replaceMethodSignature(@NotNull Method method, @NotNull List<String> newParameters, @NotNull Document document) {
        // Find the method signature in the source
        String methodText = method.getText();
        int methodStartOffset = method.getTextRange().getStartOffset();

        // Find where the method body starts (the opening brace)
        int bodyStartIndex = methodText.indexOf('{');
        if (bodyStartIndex == -1) {
            return;
        }

        // Build the new method signature
        StringBuilder newSignature = new StringBuilder("public function __invoke(");

        if (!newParameters.isEmpty()) {
            newSignature.append("\n");
            newSignature.append(String.join(",\n", newParameters));
            newSignature.append("\n");
        }

        newSignature.append("): int ");

        // Replace everything from the start of the method to the opening brace
        int endOffset = methodStartOffset + bodyStartIndex;
        document.replaceString(methodStartOffset, endOffset, newSignature.toString());
    }

    @NotNull
    private List<String> buildNewParameterList(@NotNull ParameterUsage usage, @NotNull ParameterNames paramNames, @NotNull ConfigureData configureData) {
        // Separate required and optional parameters to ensure correct ordering
        List<String> requiredParameters = new ArrayList<>();
        List<String> optionalParameters = new ArrayList<>();

        // InputInterface and OutputInterface are always required parameters
        if (usage.inputUsed) {
            requiredParameters.add(paramNames.inputType + " $" + paramNames.inputParam);
        }

        if (usage.outputUsed) {
            requiredParameters.add(paramNames.outputType + " $" + paramNames.outputParam);
        }

        // Add arguments as parameters (sort by required/optional)
        for (ArgumentInfo arg : configureData.arguments) {
            String param = buildArgumentParameter(arg);
            if (arg.isOptional) {
                optionalParameters.add(param);
            } else {
                requiredParameters.add(param);
            }
        }

        // Add options as parameters (all options are optional)
        for (OptionInfo opt : configureData.options) {
            optionalParameters.add(buildOptionParameter(opt));
        }

        // Combine: required parameters first, then optional
        List<String> parameters = new ArrayList<>();
        parameters.addAll(requiredParameters);
        parameters.addAll(optionalParameters);

        return parameters;
    }


    @NotNull
    private ParameterUsage analyzeParameterUsage(@NotNull Method executeMethod, @NotNull ParameterNames paramNames) {
        ParameterUsage usage = new ParameterUsage();

        GroupStatement groupStatement = PsiTreeUtil.findChildOfType(executeMethod, GroupStatement.class);
        if (groupStatement == null) {
            return usage;
        }

        // Simply check if the input/output variables are used anywhere in the method body
        Collection<Variable> variables = PsiTreeUtil.findChildrenOfType(groupStatement, Variable.class);

        for (Variable variable : variables) {
            String varName = variable.getName();

            if (varName.equals(paramNames.inputParam)) {
                usage.inputUsed = true;
            } else if (varName.equals(paramNames.outputParam)) {
                usage.outputUsed = true;
            }
        }

        return usage;
    }

    @NotNull
    private String buildArgumentParameter(@NotNull ArgumentInfo arg) {
        StringBuilder sb = new StringBuilder();

        // Add #[Argument] attribute - use short name, use statement will be added separately
        sb.append("#[Argument(");

        List<String> attributeParams = new ArrayList<>();

        // Convert argument name to valid PHP variable name
        String variableName = convertToValidPhpVariableName(arg.name);

        // Add name parameter if the variable name differs from the original argument name
        if (!variableName.equals(arg.name)) {
            attributeParams.add("name: '" + escapeString(arg.name) + "'");
        }

        // Add description if present
        if (StringUtils.isNotBlank(arg.description)) {
            attributeParams.add("description: '" + escapeString(arg.description) + "'");
        }

        sb.append(String.join(", ", attributeParams));
        sb.append(")]");

        // Determine type - use nullable for optional parameters
        String type = arg.isArray ? "array" : "string";
        if (arg.isOptional) {
            type = "?" + type;
        }

        // Add parameter
        sb.append(" ").append(type).append(" $").append(variableName);

        // Add default value if optional
        if (arg.isOptional) {
            if (StringUtils.isNotBlank(arg.defaultValue)) {
                sb.append(" = ").append(arg.defaultValue);
            } else {
                sb.append(" = null");
            }
        }

        return sb.toString();
    }

    @NotNull
    private String buildOptionParameter(@NotNull OptionInfo opt) {
        StringBuilder sb = new StringBuilder();

        // Add #[Option] attribute - use short name, use statement will be added separately
        sb.append("#[Option(");

        List<String> attributeParams = new ArrayList<>();

        // Convert option name to valid PHP variable name
        String variableName = convertToValidPhpVariableName(opt.name);

        // Add name parameter if the variable name differs from the original option name
        if (!variableName.equals(opt.name)) {
            attributeParams.add("name: '" + escapeString(opt.name) + "'");
        }

        // Add shortcut if present
        if (StringUtils.isNotBlank(opt.shortcut)) {
            attributeParams.add("shortcut: '" + opt.shortcut + "'");
        }

        // Add description if present
        if (StringUtils.isNotBlank(opt.description)) {
            attributeParams.add("description: '" + escapeString(opt.description) + "'");
        }

        sb.append(String.join(", ", attributeParams));
        sb.append(")]");

        // Determine type based on mode - use nullable for non-VALUE_NONE options
        String type;
        if (opt.isValueNone) {
            type = "bool";
        } else if (opt.isArray) {
            type = "?array";
        } else {
            type = "?string";
        }

        // Add parameter
        sb.append(" ").append(type).append(" $").append(variableName);

        // Add default value
        if (StringUtils.isNotBlank(opt.defaultValue)) {
            sb.append(" = ").append(opt.defaultValue);
        } else if (opt.isValueNone) {
            sb.append(" = false");
        } else {
            // For non-VALUE_NONE options without explicit default, use null
            sb.append(" = null");
        }

        return sb.toString();
    }



    @NotNull
    private String escapeString(@NotNull String str) {
        return str.replace("'", "\\'");
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
    @NotNull
    private String convertToValidPhpVariableName(@NotNull String name) {
        // Check if the name is already valid
        if (isValidPhpVariableName(name)) {
            return name;
        }

        // Split by hyphens and other non-alphanumeric characters (except underscores)
        String[] parts = name.split("[^a-zA-Z0-9_]+");

        if (parts.length == 0) {
            // Fallback if the name is completely invalid
            return "arg";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }

            if (i == 0) {
                // First part: lowercase
                result.append(parts[i].toLowerCase());
            } else {
                // Subsequent parts: capitalize first letter (camelCase)
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1).toLowerCase());
                }
            }
        }

        String finalName = result.toString();

        // Ensure it starts with a letter or underscore
        if (!finalName.isEmpty() && !Character.isLetter(finalName.charAt(0)) && finalName.charAt(0) != '_') {
            finalName = "_" + finalName;
        }

        return finalName.isEmpty() ? "arg" : finalName;
    }

    /**
     * Checks if a string is a valid PHP variable name.
     *
     * @param name The name to check
     * @return true if the name is valid, false otherwise
     */
    private boolean isValidPhpVariableName(@NotNull String name) {
        if (name.isEmpty()) {
            return false;
        }

        // First character must be a letter or underscore
        char first = name.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }

        // Remaining characters must be letters, numbers, or underscores
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }

    // Data classes
    private static class ParameterNames {
        String inputParam = "input";
        String outputParam = "output";
        String inputType = "InputInterface";
        String outputType = "OutputInterface";
    }

    private static class ParameterUsage {
        boolean inputUsed = false;
        boolean outputUsed = false;
    }

    private static class ConfigureData {
        List<ArgumentInfo> arguments = new ArrayList<>();
        List<OptionInfo> options = new ArrayList<>();
        boolean hasOtherConfigureCalls = false;

        boolean canRemoveConfigure() {
            // Only remove configure if it only contains addArgument/addOption calls
            return !hasOtherConfigureCalls;
        }
    }

    private static class ArgumentInfo {
        String name;
        String description;
        String defaultValue;
        boolean isOptional;
        boolean isArray;
    }

    private static class OptionInfo {
        String name;
        String shortcut;
        String description;
        String defaultValue;
        boolean isValueNone;
        boolean isArray;
        boolean isRequired;
    }
}
