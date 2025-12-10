package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import icons.SymfonyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Intention action to migrate TwigExtension getFilters(), getFunctions(), and getTests()
 * methods to use PHP attributes (#[AsTwigFilter], #[AsTwigFunction], #[AsTwigTest]).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtensionToAttributeIntention extends PsiElementBaseIntentionAction implements Iconable {

    private static final String ABSTRACT_EXTENSION_FQN = "\\Twig\\Extension\\AbstractExtension";
    private static final String TWIG_FILTER_FQN = "Twig\\Attribute\\AsTwigFilter";
    private static final String TWIG_FUNCTION_FQN = "Twig\\Attribute\\AsTwigFunction";
    private static final String TWIG_TEST_FQN = "Twig\\Attribute\\AsTwigTest";

    @Override
    public @Nullable FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
        return null;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PhpClass phpClass = getTwigExtensionClass(project, psiElement);
        if (phpClass == null) {
            return;
        }

        List<MethodTransformation> transformations = findTransformations(phpClass);
        if (transformations.isEmpty()) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (MethodTransformation transformation : transformations) {
                applyTransformation(project, transformation);
            }

            // After all transformations, check if we can remove the extends clause
            checkAndRemoveExtendsAbstractExtension(project, phpClass);
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        PhpClass phpClass = getTwigExtensionClass(project, psiElement);
        if (phpClass == null) {
            return false;
        }

        return hasAnyTransformableMethods(phpClass);
    }

    /**
     * Fast check to determine if the class has any of the target methods with transformable content.
     * For performance, we do a lightweight check for non-empty arrays in isAvailable().
     * The full transformation analysis happens in invoke().
     */
    private boolean hasAnyTransformableMethods(PhpClass phpClass) {
        Method getFiltersMethod = phpClass.findOwnMethodByName("getFilters");
        if (getFiltersMethod != null && hasNonEmptyReturnArray(getFiltersMethod)) {
            return true;
        }

        Method getFunctionsMethod = phpClass.findOwnMethodByName("getFunctions");
        if (getFunctionsMethod != null && hasNonEmptyReturnArray(getFunctionsMethod)) {
            return true;
        }

        Method getTestsMethod = phpClass.findOwnMethodByName("getTests");
        if (getTestsMethod != null && hasNonEmptyReturnArray(getTestsMethod)) {
            return true;
        }

        return false;
    }

    /**
     * Check if a method returns a non-empty array.
     * This is a lightweight check - we just look for NewExpression elements in the return array.
     */
    private boolean hasNonEmptyReturnArray(Method method) {
        GroupStatement groupStatement = PsiTreeUtil.findChildOfType(method, GroupStatement.class);
        if (groupStatement == null) {
            return false;
        }

        for (PsiElement child : groupStatement.getChildren()) {
            if (child instanceof PhpReturn phpReturn) {
                PsiElement returnValue = phpReturn.getArgument();
                if (returnValue instanceof ArrayCreationExpression arrayExpr) {
                    // Check if the array has any NewExpression elements
                    return PsiTreeUtil.findChildOfType(arrayExpr, NewExpression.class) != null;
                }
            }
        }

        return false;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Migrate to TwigExtension attributes";
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

    private PhpClass getTwigExtensionClass(@NotNull Project project, @NotNull PsiElement element) {
        PhpClass phpClass = PhpPsiUtil.getParentByCondition(element, true, PhpClass.INSTANCEOF, null);
        if (phpClass == null) {
            return null;
        }

        if (!PhpElementsUtil.isInstanceOf(phpClass, ABSTRACT_EXTENSION_FQN)) {
            return null;
        }

        return phpClass;
    }

    private List<MethodTransformation> findTransformations(PhpClass phpClass) {
        List<MethodTransformation> transformations = new ArrayList<>();

        // Check for getFilters() method
        Method getFiltersMethod = phpClass.findOwnMethodByName("getFilters");
        if (getFiltersMethod != null) {
            transformations.addAll(extractFilterTransformations(getFiltersMethod, phpClass));
        }

        // Check for getFunctions() method
        Method getFunctionsMethod = phpClass.findOwnMethodByName("getFunctions");
        if (getFunctionsMethod != null) {
            transformations.addAll(extractFunctionTransformations(getFunctionsMethod, phpClass));
        }

        // Check for getTests() method
        Method getTestsMethod = phpClass.findOwnMethodByName("getTests");
        if (getTestsMethod != null) {
            transformations.addAll(extractTestTransformations(getTestsMethod, phpClass));
        }

        return transformations;
    }

    private List<MethodTransformation> extractFilterTransformations(Method getFiltersMethod, PhpClass phpClass) {
        return extractTransformations(getFiltersMethod, phpClass, TWIG_FILTER_FQN, "TwigFilter");
    }

    private List<MethodTransformation> extractFunctionTransformations(Method getFunctionsMethod, PhpClass phpClass) {
        return extractTransformations(getFunctionsMethod, phpClass, TWIG_FUNCTION_FQN, "TwigFunction");
    }

    private List<MethodTransformation> extractTestTransformations(Method getTestsMethod, PhpClass phpClass) {
        return extractTransformations(getTestsMethod, phpClass, TWIG_TEST_FQN, "TwigTest");
    }

    private List<MethodTransformation> extractTransformations(Method getMethod, PhpClass phpClass, String attributeFqn, String twigType) {
        List<MethodTransformation> transformations = new ArrayList<>();

        // Find the method body (GroupStatement)
        GroupStatement groupStatement = PsiTreeUtil.findChildOfType(getMethod, GroupStatement.class);
        if (groupStatement == null) {
            return transformations;
        }

        // Look for array return values inside the method body
        for (PsiElement child : groupStatement.getChildren()) {
            if (child instanceof PhpReturn phpReturn) {
                PsiElement returnValue = phpReturn.getArgument();
                if (returnValue instanceof ArrayCreationExpression) {
                    // Parse the array for TwigFilter/TwigFunction/TwigTest instances
                    extractFromArray((ArrayCreationExpression) returnValue, transformations, twigType, attributeFqn, phpClass);
                }
            }
        }

        return transformations;
    }

    private void extractFromArray(ArrayCreationExpression arrayExpr, List<MethodTransformation> transformations, String twigType, String attributeFqn, PhpClass phpClass) {
        // Find all NewExpression elements in the array (handles both indexed and associative arrays)
        Collection<NewExpression> newExpressions = PsiTreeUtil.findChildrenOfType(arrayExpr, NewExpression.class);

        for (NewExpression newExpr : newExpressions) {
            // Check if this is a TwigFilter/TwigFunction/TwigTest instantiation
            ClassReference classReference = newExpr.getClassReference();
            if (classReference == null) {
                continue;
            }

            String className = classReference.getName();
            if (!twigType.equals(className)) {
                continue;
            }

            // Extract parameters from the NewExpression
            PsiElement[] parameters = newExpr.getParameters();
            if (parameters.length < 2) {
                continue; // Need at least name and callable
            }

            String name = null;
            String methodName = null;
            String options = "";

            // First parameter: name (string literal)
            if (parameters[0] instanceof StringLiteralExpression nameExpr) {
                name = nameExpr.getContents();
            }

            // Second parameter: callable array [$this, 'methodName'] or [SomeClass::class, 'methodName'] or first-class callable $this->method(...)
            if (parameters[1] instanceof ArrayCreationExpression callableArray) {
                // Check if this is [$this, 'methodName'] - only migrate if it references $this
                Collection<Variable> variables = PsiTreeUtil.findChildrenOfType(callableArray, Variable.class);
                boolean referencesThis = false;
                for (Variable var : variables) {
                    if ("this".equals(var.getName())) {
                        referencesThis = true;
                        break;
                    }
                }
                // Only process if it references $this (not other classes)
                if (referencesThis) {
                    Collection<StringLiteralExpression> stringLiterals = PsiTreeUtil.findChildrenOfType(callableArray, StringLiteralExpression.class);
                    // The method name is the string literal that's not 'this'
                    for (StringLiteralExpression literal : stringLiterals) {
                        String value = literal.getContents();
                        if (!value.isEmpty() && !value.equals("this")) {
                            methodName = value;
                            break;
                        }
                    }
                }
            } else if (parameters[1] instanceof PhpCallableMethod callableMethod) {
                // First-class callable syntax: $this->methodName(...)
                methodName = callableMethod.getName();
            }

            // Third parameter: options array (optional)
            if (parameters.length > 2 && parameters[2] instanceof ArrayCreationExpression optionsArray) {
                Collection<ArrayHashElement> optionElements = PsiTreeUtil.findChildrenOfType(optionsArray, ArrayHashElement.class);
                StringBuilder optionsBuilder = new StringBuilder();

                for (ArrayHashElement hashElement : optionElements) {
                    PsiElement keyElement = hashElement.getKey();
                    PsiElement valueElement = hashElement.getValue();

                    if (keyElement instanceof StringLiteralExpression keyExpr && valueElement != null) {
                        String key = keyExpr.getContents();
                        String value = valueElement.getText();

                        // Convert snake_case to camelCase for PHP attributes
                        key = StringUtils.camelize(key, true);

                        if (!optionsBuilder.isEmpty()) {
                            optionsBuilder.append(", ");
                        }
                        optionsBuilder.append(key).append(": ").append(value);
                    }
                }

                options = optionsBuilder.toString();
            }

            // Validate that we have the required data
            if (name == null || name.isEmpty() || methodName == null || methodName.isEmpty()) {
                continue;
            }

            // Find the target method in the class
            Method targetMethod = phpClass.findOwnMethodByName(methodName);
            if (targetMethod == null) {
                continue; // Method doesn't exist in the class
            }

            // Find the element to delete from the array
            // The NewExpression is wrapped in PhpPsiElementImpl, which is the array element
            PsiElement elementToDelete = newExpr.getParent();

            transformations.add(new MethodTransformation(
                name,
                methodName,
                attributeFqn,
                options,
                targetMethod,
                elementToDelete
            ));
        }
    }

    private void applyTransformation(Project project, MethodTransformation transformation) {
        PhpClass phpClass = transformation.method.getContainingClass();
        if (phpClass == null) {
            return;
        }

        // Add the attribute import using PhpElementsUtil
        String importedName = PhpElementsUtil.insertUseIfNecessary(phpClass, transformation.attributeFqn);
        if (importedName != null) {
            transformation.importedName = importedName;
        }

        // Create and add the attribute to the method
        String attributeText = createAttributeText(transformation);
        addAttributeToMethod(project, transformation.method, attributeText);

        // Remove the corresponding entry from the getMethods/getFilters/getTests array
        removeFromArray(transformation);

        // If the array becomes empty, remove the entire getXXX method
        checkAndRemoveEmptyGetMethod(phpClass, transformation.attributeFqn);
    }

    private String createAttributeText(MethodTransformation transformation) {
        String attributeName = transformation.importedName != null ?
            transformation.importedName :
            transformation.attributeFqn.substring(transformation.attributeFqn.lastIndexOf("\\") + 1);

        if (transformation.options.isEmpty()) {
            return "#[" + attributeName + "('" + transformation.name + "')]";
        } else {
            return "#[" + attributeName + "('" + transformation.name + "', " + transformation.options + ")]";
        }
    }

    private void addAttributeToMethod(Project project, Method method, String attributeText) {
        // Insert attribute text directly before the method
        PsiFile file = method.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        int methodStartOffset = method.getTextRange().getStartOffset();
        String fullAttributeText = attributeText + "\n";

        document.insertString(methodStartOffset, fullAttributeText);
        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        // Reformat the added attribute with proper indentation
        CodeUtil.reformatAddedAttribute(project, document, methodStartOffset);
    }

    private void removeFromArray(MethodTransformation transformation) {
        if (transformation.arrayElement == null) {
            return;
        }

        PsiElement prev = transformation.arrayElement.getPrevSibling();
        PsiElement next = transformation.arrayElement.getNextSibling();

        // First delete the array element itself
        transformation.arrayElement.delete();

        // Then handle comma cleanup
        // After deleting the element, we need to check if there's a dangling comma
        if (prev != null && next != null) {
            // Element was in the middle, check for comma after or before
            if (",".equals(next.getText())) {
                next.delete();
            } else if (prev.getText().endsWith(",")) {
                // Check if prev sibling is a comma or ends with comma
                if (",".equals(prev.getText())) {
                    prev.delete();
                }
            }
        } else if (prev != null && prev.getText().trim().endsWith(",")) {
            // Last element, remove trailing comma from previous element
            if (",".equals(prev.getText().trim())) {
                prev.delete();
            }
        } else if (next != null && ",".equals(next.getText().trim())) {
            // First element, remove leading comma
            next.delete();
        }
    }

    private void checkAndRemoveEmptyGetMethod(@NotNull PhpClass phpClass, @NotNull String attributeFqn) {
        String methodName = null;
        if (attributeFqn.equals(TWIG_FILTER_FQN)) {
            methodName = "getFilters";
        } else if (attributeFqn.equals(TWIG_FUNCTION_FQN)) {
            methodName = "getFunctions";
        } else if (attributeFqn.equals(TWIG_TEST_FQN)) {
            methodName = "getTests";
        }

        if (methodName == null) {
            return;
        }

        Method getMethod = phpClass.findOwnMethodByName(methodName);
        if (getMethod == null) {
            return;
        }

        // Check if the method returns an empty array
        // Need to look inside GroupStatement for the return statement
        GroupStatement groupStatement = PsiTreeUtil.findChildOfType(getMethod, GroupStatement.class);
        if (groupStatement == null) {
            return;
        }

        boolean isEmpty = false;
        for (PsiElement child : groupStatement.getChildren()) {
            if (child instanceof PhpReturn phpReturn) {
                PsiElement returnValue = phpReturn.getArgument();
                if (returnValue instanceof ArrayCreationExpression arrayExpr) {
                    // Check if the array has no NewExpression elements
                    isEmpty = PsiTreeUtil.findChildOfType(arrayExpr, NewExpression.class) == null;
                }
            }
        }

        if (isEmpty) {
            getMethod.delete();
        }
    }

    private void checkAndRemoveExtendsAbstractExtension(@NotNull Project project, PhpClass phpClass) {
        // Check if getFilters, getFunctions, and getTests methods are all removed
        if (phpClass.findOwnMethodByName("getFilters") != null ||
            phpClass.findOwnMethodByName("getFunctions") != null ||
            phpClass.findOwnMethodByName("getTests") != null) {
            // Some get methods still exist, don't remove extends
            return;
        }

        // Check if the class extends AbstractExtension
        String superClass = phpClass.getSuperClass() != null ? phpClass.getSuperClass().getFQN() : null;
        if (!ABSTRACT_EXTENSION_FQN.equals(superClass) && !"AbstractExtension".equals(superClass)) {
            // Doesn't extend AbstractExtension, nothing to do
            return;
        }

        // Remove the extends clause using shared utility method
        if (CodeUtil.removeExtendsClause(phpClass, ABSTRACT_EXTENSION_FQN)) {
            // Optimize imports to remove unused AbstractExtension import
            optimizeImports(phpClass.getContainingFile());
        }
    }

    private void optimizeImports(@NotNull PsiFile file) {
        var optimizers = com.intellij.lang.LanguageImportStatements.INSTANCE.forFile(file);
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

    private static class MethodTransformation {
        final String name;
        final String methodName;
        final String attributeFqn;
        final String options;
        final Method method;
        final PsiElement arrayElement;
        String importedName;

        MethodTransformation(String name, String methodName, String attributeFqn, String options, Method method, PsiElement arrayElement) {
            this.name = name;
            this.methodName = methodName;
            this.attributeFqn = attributeFqn;
            this.options = options;
            this.method = method;
            this.arrayElement = arrayElement;
        }
    }
}