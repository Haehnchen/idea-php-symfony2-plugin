package fr.adrienbrault.idea.symfony2plugin.lang;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ParameterListImpl;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LanguageInjectionPatterns {

    public static ElementPattern<? extends PsiElement> getAnnotationPropertyPattern(
        @NotNull String classFQN,
        @NotNull String propertyName,
        boolean isDefaultProperty) {
        return PlatformPatterns
            .psiElement(StringLiteralExpression.class)
            .with(new IsAnnotationProperty(classFQN, propertyName, isDefaultProperty));
    }

    public static ElementPattern<? extends PsiElement> getAttributeArgumentPattern(
        @NotNull String classFQN,
        @NotNull String argumentName,
        int argumentIndex
    ) {
        return PlatformPatterns.psiElement()
            .with(new IsArgument(argumentName, argumentIndex))
            .withParent(PlatformPatterns
                .psiElement(ParameterList.class)
                .withParent(
                    PlatformPatterns.psiElement(PhpAttribute.class)
                        .with(new IsAttribute(classFQN))
                )
            );
    }

    public static ElementPattern<? extends PsiElement> getMethodCallArgumentPattern(
        @NotNull String classFQN,
        @NotNull String methodName,
        @NotNull String argumentName,
        int argumentIndex
    ) {
        return PlatformPatterns.psiElement()
            .with(new IsArgument(argumentName, argumentIndex))
            .withParent(PlatformPatterns
                .psiElement(ParameterList.class)
                .withParent(
                    PlatformPatterns.psiElement(MethodReference.class)
                        .with(new IsMethodReference(classFQN, methodName))
                )
            );
    }

    public static ElementPattern<? extends PsiElement> getConstructorCallArgumentPattern(
        @NotNull String classFQN,
        @NotNull String argumentName,
        int argumentIndex
    ) {
        return PlatformPatterns.psiElement()
            .with(new IsArgument(argumentName, argumentIndex))
            .withParent(PlatformPatterns
                .psiElement(ParameterList.class)
                .withParent(
                    PlatformPatterns.psiElement(NewExpression.class)
                        .with(new IsConstructorReference(classFQN))
                )
            );
    }

    public static ElementPattern<? extends PsiElement> getConstructorCallWithArrayArgumentPattern(
        @NotNull String classFQN,
        @NotNull String argumentName,
        int argumentIndex,
        @NotNull String keyName
    ) {
        return PlatformPatterns.psiElement()
            .withParent(PlatformPatterns
                .psiElement(PhpPsiElement.class)
                .withElementType(PhpElementTypes.ARRAY_VALUE)
                .withParent(PlatformPatterns
                    .psiElement(ArrayHashElement.class)
                    .with(new IsArrayHashElementKey(keyName))
                    .withParent(PlatformPatterns
                        .psiElement(ArrayCreationExpression.class)
                        .with(new IsArgument(argumentName, argumentIndex))
                        .withParent(PlatformPatterns
                            .psiElement(ParameterList.class)
                            .withParent(
                                PlatformPatterns.psiElement(NewExpression.class)
                                    .with(new IsConstructorReference(classFQN))
                            )
                        )
                    )
                )
            );
    }

    public static ElementPattern<? extends PsiElement> getFunctionCallArgumentPattern(
        @NotNull String functionFQN,
        @NotNull String argumentName,
        int argumentIndex
    ) {
        return PlatformPatterns.psiElement()
            .with(new IsArgument(argumentName, argumentIndex))
            .withParent(PlatformPatterns
                .psiElement(ParameterList.class)
                .withParent(
                    PlatformPatterns.psiElement(FunctionReference.class)
                        .with(new IsFunctionReference(functionFQN))
                )
            );
    }

    public static ElementPattern<? extends PsiElement> getVariableAssignmentPattern(@NotNull String variableName) {
        return PlatformPatterns.psiElement()
            .withParent(PlatformPatterns
                .psiElement(AssignmentExpression.class)
                .withFirstChild(PlatformPatterns
                    .psiElement(Variable.class)
                    .withName(variableName)
                )
            );
    }

    private static class IsAnnotationProperty extends PatternCondition<StringLiteralExpression> {
        @NotNull
        private final String classFQN;
        @NotNull
        private final String propertyName;
        private final boolean isDefaultProperty;

        public IsAnnotationProperty(@NotNull String classFQN, @NotNull String propertyName, boolean isDefaultProperty) {
            super(String.format("IsAnnotationProperty(%s, %s)", classFQN, propertyName));
            this.classFQN = classFQN;
            this.propertyName = propertyName;
            this.isDefaultProperty = isDefaultProperty;
        }

        @Override
        public boolean accepts(@NotNull StringLiteralExpression element, ProcessingContext context) {
            if (element.getParent() == null || !(element.getParent().getParent() instanceof PhpDocTag)) {
                return false;
            }

            var phpDocTag = (PhpDocTag) element.getParent().getParent();

            var annotationClass = AnnotationUtil.getAnnotationReference(phpDocTag);
            if (annotationClass != null && annotationClass.getFQN().equals(classFQN)) {
                return element.equals(getPropertyValuePsiElement(phpDocTag));
            }

            return false;
        }

        @Nullable
        private PsiElement getPropertyValuePsiElement(@NotNull PhpDocTag phpDocTag) {
            PsiElement property = AnnotationUtil.getPropertyValueAsPsiElement(phpDocTag, propertyName);

            if (property == null && isDefaultProperty) {
                var phpDocAttrList = phpDocTag.getFirstPsiChild();
                if (phpDocAttrList != null && phpDocAttrList.getNode().getElementType() == PhpDocElementTypes.phpDocAttributeList) {
                    PhpPsiElement firstPhpPsiElement = phpDocAttrList.getFirstPsiChild();
                    if (firstPhpPsiElement instanceof StringLiteralExpression && !hasIdentifier(firstPhpPsiElement)) {
                        property = firstPhpPsiElement;
                    }
                }
            }

            return property;
        }

        private boolean hasIdentifier(@NotNull PsiElement property) {
            return PlatformPatterns.psiElement()
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(PhpDocTokenTypes.DOC_TEXT).withText("=")
                    ),
                    PlatformPatterns.psiElement(PhpDocTokenTypes.DOC_IDENTIFIER)
                )
                .accepts(property);
        }
    }

    private static class IsAttribute extends PatternCondition<PhpAttribute> {
        @NotNull
        private final String classFQN;

        public IsAttribute(@NotNull String classFQN) {
            super(String.format("IsAttribute(%s)", classFQN));
            this.classFQN = classFQN;
        }

        @Override
        public boolean accepts(@NotNull PhpAttribute phpAttribute, ProcessingContext context) {
            return classFQN.equals(phpAttribute.getFQN());
        }
    }

    private static class IsArrayHashElementKey extends PatternCondition<ArrayHashElement> {
        @NotNull
        private final String name;

        public IsArrayHashElementKey(@NotNull String name) {
            super(String.format("IsArrayHashElementKey(%s)", name));
            this.name = name;
        }

        @Override
        public boolean accepts(@NotNull ArrayHashElement arrayHashElement, ProcessingContext context) {
            var key = arrayHashElement.getKey();
            if (key instanceof StringLiteralExpression) {
                return ((StringLiteralExpression) key).getContents().equals(name);
            }

            return false;
        }
    }

    private static class IsArgument extends PatternCondition<PsiElement> {
        @NotNull
        private final String name;
        private final int index;

        public IsArgument(@NotNull String name, int index) {
            super(String.format("isArgument(%s, %d)", name, index));
            this.name = name;
            this.index = index;
        }

        @Override
        public boolean accepts(@NotNull PsiElement parameter, ProcessingContext context) {
            if (parameter.getParent() instanceof ParameterListImpl) {
                var parameterList = (ParameterListImpl) parameter.getParent();
                if (parameterList.getParameter(name) == parameter) {
                    return true;
                }

                return parameterList.getParameter(index) == parameter && ParameterListImpl.getNameIdentifier(parameter) == null;
            }

            return false;
        }
    }

    private static class IsFunctionReference extends PatternCondition<FunctionReference> {
        @NotNull
        private final String name;

        public IsFunctionReference(@NotNull String name) {
            super(String.format("IsFunctionReference(%s)", name));
            this.name = name;
        }

        @Override
        public boolean accepts(@NotNull FunctionReference element, ProcessingContext context) {
            return name.equals(element.getFQN());
        }
    }

    private static class IsMethodReference extends PatternCondition<MethodReference> {
        @NotNull
        private final String classFQN;
        @NotNull
        private final String methodName;

        public IsMethodReference(@NotNull String classFQN, @NotNull String methodName) {
            super(String.format("IsMethodReference(%s::%s)", classFQN, methodName));
            this.classFQN = classFQN;
            this.methodName = methodName;
        }

        @Override
        public boolean accepts(@NotNull MethodReference element, ProcessingContext context) {
            return PhpElementsUtil.isMethodReferenceInstanceOf(element, classFQN, methodName);
        }
    }

    private static class IsConstructorReference extends PatternCondition<NewExpression> {
        @NotNull
        private final String classFQN;

        public IsConstructorReference(@NotNull String classFQN) {
            super(String.format("IsConstructorReference(%s)", classFQN));
            this.classFQN = classFQN;
        }

        @Override
        public boolean accepts(@NotNull NewExpression newExpression, ProcessingContext context) {
            return PhpElementsUtil.isNewExpressionPhpClassWithInstance(newExpression, classFQN);
        }
    }
}
