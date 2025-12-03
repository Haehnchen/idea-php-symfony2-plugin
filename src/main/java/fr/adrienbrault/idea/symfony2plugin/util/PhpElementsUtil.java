package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.jetbrains.php.PhpClassHierarchyUtils;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.codeInsight.PhpScopeHolder;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpAccessVariableInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpCallInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpConstructorCallInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpReturnInstruction;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstImpl;
import com.jetbrains.php.lang.psi.elements.impl.ConstantImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpDefineImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionArgument;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionClassConstantArgument;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionScalarArgument;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import com.jetbrains.php.refactoring.PhpAliasImporter;
import fr.adrienbrault.idea.symfony2plugin.dic.MethodReferenceBag;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import kotlin.Pair;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpElementsUtil {

    /**
     * Only parameter on first index or named: "a('caret'), a(test: 'caret')"
     */
    private static final PatternCondition<StringLiteralExpression> WITH_PREVIOUS_WHITESPACE_OR_COLON = new PatternCondition<>("whitespace or colon") {
        @Override
        public boolean accepts(@NotNull StringLiteralExpression element, ProcessingContext context) {
            ASTNode previousNonWhitespaceSibling = FormatterUtil.getPreviousNonWhitespaceSibling(element.getNode());
            return previousNonWhitespaceSibling == null || previousNonWhitespaceSibling.getElementType() == PhpTokenTypes.opCOLON;
        }
    };

    /**
     * Gets all array keys as string of an ArrayCreationExpression
     *
     * ['foo' => $bar]
     */
    @NotNull
    static public Collection<String> getArrayCreationKeys(@NotNull ArrayCreationExpression arrayCreationExpression) {
        return getArrayCreationKeyMap(arrayCreationExpression).keySet();
    }

    /**
     * Gets array key-value as single PsiElement map
     *
     * ['foo' => $bar]
     */
    @NotNull
    static public Map<String, PsiElement> getArrayCreationKeyMap(@NotNull ArrayCreationExpression arrayCreationExpression) {
        Map<String, PsiElement> keys = new HashMap<>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child instanceof StringLiteralExpression) {
                keys.put(((StringLiteralExpression) child).getContents(), child);
            }
            if(child instanceof ClassConstantReference) {
                PsiElement val = ((ClassConstantReference) child).resolve();
                if (val instanceof ClassConstImpl) {
                    PsiElement value = ((ClassConstImpl) val).getDefaultValue();
                    if (value != null && value.getText() != null) {
                        keys.put(value.getText().replace("\"", "").replace("\'", ""), child);
                    }
                }
            }
            if(child instanceof ConstantReference) {
                PsiElement val = ((ConstantReference) child).resolve();
                if(val instanceof PhpDefine) {
                    PhpPsiElement value = ((PhpDefineImpl) val).getValue();
                    if (value != null) {
                        keys.put(value.getText().replace("\"", "").replace("\'", ""), child);
                    }
                }
                if(val instanceof ConstantImpl) {
                    PsiElement value = ((ConstantImpl) val).getValue();
                    if (value != null && value.getText() != null) {
                        keys.put(value.getText().replace("\"", "").replace("\'", ""), child);
                    }
                }
            }
        }

        return keys;
    }

    /**
     * Gets string values of array
     *
     * ["value", "value2"]
     */
    @NotNull
    static public Set<String> getArrayValuesAsString(@NotNull ArrayCreationExpression arrayCreationExpression) {
        return getArrayValuesAsMap(arrayCreationExpression).keySet();
    }

    /**
     * Get array string values mapped with their PsiElements
     *
     * ["value", "value2"]
     */
    @NotNull
    static public Map<String, PsiElement> getArrayValuesAsMap(@NotNull ArrayCreationExpression arrayCreationExpression) {
        Collection<PsiElement> arrayValues = PhpPsiUtil.getChildren(arrayCreationExpression, psiElement ->
            psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE
        );

        Map<String, PsiElement> keys = new HashMap<>();
        for (PsiElement child : arrayValues) {
            String stringValue = PhpElementsUtil.getStringValue(child.getFirstChild());
            if(StringUtils.isNotBlank(stringValue)) {
                keys.put(stringValue, child);
            }
        }

        return keys;
    }

    /**
     * Get array values
     *
     * ["value", FOO::class] but not [$foo . $foo, $foo]
     */
    @NotNull
    static public PsiElement[] getArrayValues(@NotNull ArrayCreationExpression arrayCreationExpression) {
        Collection<PsiElement> arrayValues = PhpPsiUtil.getChildren(arrayCreationExpression, psiElement ->
            psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE
        );

        List<PsiElement> items = new ArrayList<>();
        for (PsiElement child : arrayValues) {
            if (child instanceof PhpPsiElement) {
                PsiElement[] children = child.getChildren();
                if (children.length == 1) {
                    items.add(children[0]);
                } else {
                    // inalid for use: [$foo . $foo]
                    return new PsiElement[0];
                }
            }
        }

        return items.toArray(new PsiElement[0]);
    }

    /**
     * array('foo' => FOO.class, 'foo1' => 'bar', 1 => 'foo')
     */
    @NotNull
    static public Map<String, PsiElement> getArrayKeyValueMapWithValueAsPsiElement(@NotNull ArrayCreationExpression arrayCreationExpression) {
        HashMap<String, PsiElement> keys = new HashMap<>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child != null && ((child instanceof StringLiteralExpression) || PhpPatterns.psiElement(PhpElementTypes.NUMBER).accepts(child))) {

                String key;
                if(child instanceof StringLiteralExpression) {
                    key = ((StringLiteralExpression) child).getContents();
                } else {
                    key = child.getText();
                }

                if(key == null || StringUtils.isBlank(key)) {
                    continue;
                }

                keys.put(key, arrayHashElement.getValue());
            }
        }

        return keys;
    }

    /**
     * array('foo' => FOO.class, 'foo1' => 'bar', 1 => 'foo')
     */
    @NotNull
    static public Map<String, Pair<PsiElement, PsiElement>> getArrayKeyValueMapWithKeyAndValueElement(@NotNull ArrayCreationExpression arrayCreationExpression) {
        HashMap<String, Pair<PsiElement, PsiElement>> keys = new HashMap<>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child != null && ((child instanceof StringLiteralExpression) || PhpPatterns.psiElement(PhpElementTypes.NUMBER).accepts(child))) {

                String key;
                if(child instanceof StringLiteralExpression) {
                    key = ((StringLiteralExpression) child).getContents();
                } else {
                    key = child.getText();
                }

                if(key == null || StringUtils.isBlank(key)) {
                    continue;
                }

                keys.put(key, new Pair(arrayHashElement.getKey(), arrayHashElement.getValue()));
            }
        }

        return keys;
    }

    /**
     * array('foo' => 'bar', 'foo1' => 'bar', 1 => 'foo')
     */
    @NotNull
    static public HashMap<String, String> getArrayKeyValueMap(@NotNull ArrayCreationExpression arrayCreationExpression) {
        HashMap<String, String> keys = new HashMap<>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child != null && ((child instanceof StringLiteralExpression) || PhpPatterns.psiElement(PhpElementTypes.NUMBER).accepts(child))) {

                String key;
                if(child instanceof StringLiteralExpression) {
                    key = ((StringLiteralExpression) child).getContents();
                } else {
                    key = child.getText();
                }

                if(key == null || StringUtils.isBlank(key)) {
                    continue;
                }

                String value = null;

                if(arrayHashElement.getValue() instanceof StringLiteralExpression) {
                    value = ((StringLiteralExpression) arrayHashElement.getValue()).getContents();
                }

                if(value == null || StringUtils.isBlank(value)) {
                    continue;
                }

                keys.put(key, value);
            }
        }

        return keys;
    }

    @Nullable
    static public PhpPsiElement getArrayValue(ArrayCreationExpression arrayCreationExpression, String name) {

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child instanceof StringLiteralExpression) {
                if(((StringLiteralExpression) child).getContents().equals(name)) {
                    return arrayHashElement.getValue();
                }
            }
        }

        return null;
    }

    @Nullable
    static public String getArrayValueString(@NotNull ArrayCreationExpression arrayCreationExpression, @NotNull String name) {
        PhpPsiElement phpPsiElement = getArrayValue(arrayCreationExpression, name);
        if(phpPsiElement == null) {
            return null;
        }

        if(phpPsiElement instanceof StringLiteralExpression) {
            return ((StringLiteralExpression) phpPsiElement).getContents();
        }

        return null;
    }

    @Nullable
    static public Boolean getArrayValueBool(@NotNull ArrayCreationExpression arrayCreationExpression, @NotNull String name) {
        PhpPsiElement phpPsiElement = getArrayValue(arrayCreationExpression, name);
        if(phpPsiElement == null) {
            return null;
        }

        if (phpPsiElement instanceof ConstantReference) {
            String lowerCase = phpPsiElement.getText().toLowerCase();
            if (lowerCase.equals("true")) {
                return true;
            } else if (lowerCase.equals("false")) {
                return false;
            }
        }

        return null;
    }

    static public PhpNamedElement[] getPsiElementsBySignature(@NotNull Project project, @Nullable String signature) {

        if(signature == null) {
            return new PhpNamedElement[0];
        }

        Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpIndex.getInstance(project).getBySignature(signature, null, 0);
        return phpNamedElementCollections.toArray(new PhpNamedElement[0]);
    }

    @Nullable
    static public PhpNamedElement getPsiElementsBySignatureSingle(Project project, @Nullable String signature) {
        PhpNamedElement[] psiElements = getPsiElementsBySignature(project, signature);
        if(psiElements.length == 0) {
            return null;
        }

        return psiElements[0];
    }

    @Nullable
    static public Method getClassMethod(@NotNull Project project, @NotNull String phpClassName, @NotNull String methodName) {

        // we need here an each; because eg Command is non unique because phar file
        for(PhpClass phpClass: PhpIndex.getInstance(project).getClassesByFQN(phpClassName)) {
            Method method = phpClass.findMethodByName(methodName);
            if(method != null) {
                return method;
            }
        }

        return null;
    }

    /**
     * $this->methodName('service_name')
     * $this->methodName(SERVICE::NAME)
     * $this->methodName($this->name)
     */
    static public boolean isMethodWithFirstStringOrFieldReference(PsiElement psiElement, String... methodName) {

        if(!PlatformPatterns
            .psiElement(PhpElementTypes.METHOD_REFERENCE)
            .withChild(PlatformPatterns
                .psiElement(PhpElementTypes.PARAMETER_LIST)
                .withFirstChild(PlatformPatterns.or(
                    PlatformPatterns.psiElement(PhpElementTypes.STRING),
                    PlatformPatterns.psiElement(PhpElementTypes.FIELD_REFERENCE),
                    PlatformPatterns.psiElement(PhpElementTypes.CLASS_CONSTANT_REFERENCE)
                ))
            ).accepts(psiElement)) {

            return false;
        }

        // cant we move it up to PlatformPatterns? withName condition dont looks working
        String methodRefName = ((MethodReference) psiElement).getName();

        return null != methodRefName && Arrays.asList(methodName).contains(methodRefName);
    }

    /**
     *
     * @deprecated use getMethodWithFirstStringOrNamedArgumentPattern
     */
    @Deprecated
    static public PsiElementPattern.Capture<StringLiteralExpression> getMethodWithFirstStringPattern() {
        return PlatformPatterns
            .psiElement(StringLiteralExpression.class)
            .withParent(
                PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
                    .withFirstChild(
                        PlatformPatterns.psiElement(PhpElementTypes.STRING)
                    )
                    .withParent(
                        PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE)
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE);
    }

    /**
     * "$var->format(x: 'te<caret>st')"
     * "$var->format($x, 'te<caret>st')"
     * "$var->format('te<caret>st')"
     *
     * "not $var->format('', 'te<caret>st')"
     */
    static public PsiElementPattern.Capture<StringLiteralExpression> getMethodWithFirstStringOrNamedArgumentPattern() {
        return PlatformPatterns
            .psiElement(StringLiteralExpression.class)
            .withParent(
                PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
            )
            .with(WITH_PREVIOUS_WHITESPACE_OR_COLON)
            .withLanguage(PhpLanguage.INSTANCE);
    }

    /**
     * "$var->format(x: 'te<caret>st')"
     * "$var->format($x, 'te<caret>st')"
     * "$var->format('te<caret>st')"
     */
    static public PsiElementPattern.Capture<StringLiteralExpression> getMethodParameterListStringPattern() {
        return PlatformPatterns
            .psiElement(StringLiteralExpression.class)
            .withParent(
                PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST).withParent(
                    PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE)
                )
            )
            .withLanguage(PhpLanguage.INSTANCE);
    }

    static public PsiElementPattern.Capture<StringLiteralExpression> getFunctionWithFirstStringPattern(@NotNull String... functionName) {
        return PlatformPatterns
            .psiElement(StringLiteralExpression.class)
            .withParent(
                PlatformPatterns.psiElement(ParameterList.class)
                    .withFirstChild(
                        PlatformPatterns.psiElement(PhpElementTypes.STRING)
                    )
                    .withParent(
                        PlatformPatterns.psiElement(FunctionReference.class).with(new PatternCondition<>("function match") {
                            @Override
                            public boolean accepts(@NotNull FunctionReference functionReference, ProcessingContext processingContext) {
                                return ArrayUtils.contains(functionName, functionReference.getName());
                            }
                        })
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE);
    }

    public static final PatternCondition<PsiElement> EMPTY_PREVIOUS_LEAF = new PatternCondition<>("previous leaf empty") {
        @Override
        public boolean accepts(@NotNull PsiElement stringLiteralExpression, ProcessingContext context) {
            return stringLiteralExpression.getPrevSibling() == null;
        }
    };

    /**
     * #[Security("is_granted('POST_SHOW')")]
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> getFirstAttributeStringPattern(@NotNull String clazz) {
        return PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE,
                PhpTokenTypes.STRING_LITERAL
            ))
            .withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
                .with(EMPTY_PREVIOUS_LEAF)
                .withParent(PlatformPatterns.psiElement(ParameterList.class)
                    .withParent(PlatformPatterns.psiElement(PhpAttribute.class)
                        .with(new AttributeInstancePatternCondition(clazz))
                    )
                )
            );
    }

    /**
     * #[Security("is_granted(['POST_SHOW'])")]
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> getFirstAttributeArrayStringPattern(@NotNull String clazz) {
        return PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE,
                PhpTokenTypes.STRING_LITERAL
            ))
            .withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).withParent(
                    PlatformPatterns.psiElement(ArrayCreationExpression.class).with(EMPTY_PREVIOUS_LEAF).withParent(PlatformPatterns.psiElement(ParameterList.class)
                        .withParent(PlatformPatterns.psiElement(PhpAttribute.class)
                            .with(new AttributeInstancePatternCondition(clazz))
                        )
                    )
                ))
            );
    }

    /**
     * #[Security(foobar: "is_granted('POST_SHOW')")]
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> getAttributeNamedArgumentStringPattern(@NotNull String clazz, @NotNull String namedArgument) {
        return PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE,
                PhpTokenTypes.STRING_LITERAL
            ))
            .withParent(getAttributeNamedArgumentStringLiteralPattern(clazz, namedArgument));
    }

    /**
     * #[Security(foobar: "is_granted('POST_SHOW')")]
     */
    public static PsiElementPattern.@NotNull Capture<StringLiteralExpression> getAttributeNamedArgumentStringLiteralPattern(@NotNull String clazz, @NotNull String namedArgument) {
        return PlatformPatterns.psiElement(StringLiteralExpression.class)
                .afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(PhpTokenTypes.opCOLON).afterLeafSkipping(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(PhpTokenTypes.IDENTIFIER).withText(namedArgument)
                    )
                )
                .withParent(PlatformPatterns.psiElement(ParameterList.class)
                    .withParent(PlatformPatterns.psiElement(PhpAttribute.class)
                        .with(new AttributeInstancePatternCondition(clazz))
                    )
                );
    }

    /**
     * "#[FOO(namedArgument: '<caret>'])]"
     */
    public static PsiElementPattern.@NotNull Capture<StringLiteralExpression> getAttributeNamedArgumentStringLiteralPattern(@NotNull String namedArgument) {
        return PlatformPatterns.psiElement(StringLiteralExpression.class)
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(PhpTokenTypes.opCOLON).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(PhpTokenTypes.IDENTIFIER).withText(namedArgument)
                )
            )
            .withParent(PlatformPatterns.psiElement(ParameterList.class)
                .withParent(PlatformPatterns.psiElement(PhpAttribute.class))
            );
    }

    /**
     * #[Security(tags: ['foobar']])]
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> getAttributeNamedArgumentArrayStringPattern(@NotNull String clazz, @NotNull String namedArgument) {
        return PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE,
                PhpTokenTypes.STRING_LITERAL
            ))
            .withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).withParent(
                    PlatformPatterns.psiElement(ArrayCreationExpression.class).afterLeafSkipping(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(PhpTokenTypes.opCOLON).afterLeafSkipping(
                                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                                PlatformPatterns.psiElement(PhpTokenTypes.IDENTIFIER).withText(namedArgument)
                            )
                        )
                        .withParent(PlatformPatterns.psiElement(ParameterList.class)
                            .withParent(PlatformPatterns.psiElement(PhpAttribute.class)
                                .with(new AttributeInstancePatternCondition(clazz))
                            )
                        )
                ))
            );
    }

    /**
     * Collect all function references with the given name(s) inside a control flow
     *
     * @param phpScopeHolder the scope holder (method, function, etc.)
     * @param functionName   the function name(s) to search for
     * @return collection of function references found
     */
    @NotNull
    public static Collection<FunctionReference> collectFunctionReferencesInsideControlFlow(@NotNull PhpScopeHolder phpScopeHolder, @NotNull String... functionName) {
        Collection<FunctionReference> functionReferences = new ArrayList<>();

        PhpControlFlowUtil.processFlow(phpScopeHolder.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processPhpCallInstruction(PhpCallInstruction instruction) {
                if (instruction.getFunctionReference() instanceof FunctionReference funcReference && Arrays.stream(functionName).anyMatch(s -> s.equals(funcReference.getName()))) {
                    functionReferences.add(funcReference);
                }
                return super.processPhpCallInstruction(instruction);
            }
        });

        return functionReferences;
    }

    /**
     * Get deprecation message from a PHP class
     * Supports both @deprecated annotation and #[Deprecated] attribute
     *
     * @param phpClass the PHP class to check
     * @return deprecation message or null if not deprecated or no message provided
     */
    @Nullable
    public static String getClassDeprecatedMessage(@NotNull PhpClass phpClass) {
        // Check for #[Deprecated] attribute (PHP 8.4+)
        for (PhpAttribute attribute : phpClass.getAttributes()) {
            String attributeFQN = attribute.getFQN();
            if (attributeFQN != null && isEqualClassName(attributeFQN, "Deprecated")) {
                // Try named parameter first: #[Deprecated(message: 'text')]
                String message = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, "message");

                // Try positional parameter: #[Deprecated('text')]
                if (StringUtils.isBlank(message)) {
                    message = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "message");
                }

                if (StringUtils.isNotBlank(message)) {
                    return StringUtils.abbreviate("Deprecated: " + message, 100);
                }

                // Attribute found but no message provided
                return null;
            }
        }

        // Check for @deprecated doc comment
        if (phpClass.isDeprecated()) {
            PhpDocComment docComment = phpClass.getDocComment();
            if (docComment != null) {
                for (PhpDocTag deprecatedTag : docComment.getTagElementsByName("@deprecated")) {
                    String tagValue = deprecatedTag.getText();
                    if (StringUtils.isNotBlank(tagValue)) {
                        String trim = tagValue.replace("@deprecated", "").trim();
                        if (StringUtils.isNotBlank(trim)) {
                            return StringUtils.abbreviate("Deprecated: " + trim, 100);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if given Attribute
     */
    private static class AttributeInstancePatternCondition extends PatternCondition<PsiElement> {
        private final String clazz;

        AttributeInstancePatternCondition(@NotNull String clazz) {
            super("Attribute Instance");
            this.clazz = clazz;
        }

        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
            return clazz.equals(((PhpAttribute) psiElement).getFQN());
        }
    }

    /**
     * $foo->bar('<caret>')
     */
    static public PsiElementPattern.Capture<PsiElement> getParameterInsideMethodReferencePattern() {
        return PlatformPatterns
            .psiElement()
            .withParent(
                PlatformPatterns.psiElement(StringLiteralExpression.class)
                    .withParent(
                        PlatformPatterns.psiElement(ParameterList.class)
                            .withParent(
                                PlatformPatterns.psiElement(MethodReference.class)
                            )
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE);
    }

    static public PsiElementPattern.Capture<PsiElement> getParameterInsideNewExpressionPattern() {
        return PlatformPatterns
            .psiElement()
            .withParent(
                PlatformPatterns.psiElement(StringLiteralExpression.class)
                    .withParent(
                        PlatformPatterns.psiElement(ParameterList.class)
                            .withParent(
                                PlatformPatterns.psiElement(NewExpression.class)
                            )
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE);
    }

    static public PsiElementPattern.Capture<PsiElement> getParameterInsideFunctionReferencePattern() {
        return PlatformPatterns
            .psiElement()
            .withParent(
                PlatformPatterns.psiElement(StringLiteralExpression.class)
                    .withParent(
                        PlatformPatterns.psiElement(ParameterList.class)
                            .withParent(
                                PlatformPatterns.psiElement(FunctionReference.class)
                            )
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE);
    }

    /**
     * class "Foo" extends
     */
    static public PsiElementPattern.Capture<PsiElement> getClassNamePattern() {
        return PlatformPatterns
            .psiElement(PhpTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(PhpTokenTypes.kwCLASS)
            )
            .withParent(PhpClass.class)
            .withLanguage(PhpLanguage.INSTANCE);
    }

    /**
     * class Foo { function "test" {} }
     */
    static public PsiElementPattern.Capture<PsiElement> getClassMethodNamePattern() {
        return PlatformPatterns
            .psiElement(PhpTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(PhpTokenTypes.kwFUNCTION)
            )
            .withParent(Method.class)
            .withLanguage(PhpLanguage.INSTANCE);
    }

    /**
     * return 'value' inside class method
     */
    static public ElementPattern<PhpExpression> getMethodReturnPattern() {
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(PlatformPatterns.psiElement(PhpReturn.class).inside(Method.class))
                .withLanguage(PhpLanguage.INSTANCE),
            PlatformPatterns.psiElement(ClassConstantReference.class)
                .withParent(PlatformPatterns.psiElement(PhpReturn.class).inside(Method.class))
                .withLanguage(PhpLanguage.INSTANCE)
        );
    }

    /**
     * Find a string return value of a method context "function() { return 'foo'}"
     * First match wins
     */
    @Nullable
    static public String getMethodReturnAsString(@NotNull PhpClass phpClass, @NotNull String methodName) {
        final Collection<String> values = getMethodReturnAsStrings(phpClass, methodName);
        if(values.isEmpty()) {
            return null;
        }

        // we support only first item
        return values.iterator().next();
    }

    /**
     * Find a string return value of a method context "function() { return 'foo'}"
     */
    @NotNull
    public static Collection<String> getMethodReturnAsStrings(@NotNull PhpClass phpClass, @NotNull String methodName) {
        Method method = phpClass.findMethodByName(methodName);
        if(method == null) {
            return Collections.emptyList();
        }

        Set<String> values = new HashSet<>();

        PhpControlFlowUtil.processFlow(method.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processReturnInstruction(PhpReturnInstruction instruction) {
                PsiElement element = instruction.getArgument();
                
                if(PhpElementsUtil.getMethodReturnPattern().accepts(element)) {
                    String value = PhpElementsUtil.getStringValue(element);
                    if(StringUtils.isNotBlank(value)) {
                        values.add(value);
                    }
                }

                return super.processReturnInstruction(instruction);
            }
        });

        return values;
    }

    @Nullable
    static public PhpClass getClass(Project project, String className) {
        return getClass(PhpIndex.getInstance(project), className);
    }

    @Nullable
    static public PhpClass getClass(PhpIndex phpIndex, String className) {
        Collection<PhpClass> classes = phpIndex.getClassesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Nullable
    static public PhpClass getInterface(PhpIndex phpIndex, String className) {
        Collection<PhpClass> classes = phpIndex.getInterfacesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Nullable
    static public PhpClass getClassInterface(Project project, @NotNull String className) {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(project).getAnyByFQN(className);
        return phpClasses.isEmpty() ? null : phpClasses.iterator().next();
    }

    /**
     * @param subjectClass eg DateTime
     * @param expectedClass eg DateTimeInterface
     */
    public static boolean isInstanceOf(@NotNull PhpClass subjectClass, @NotNull PhpClass expectedClass) {
        if (PhpLangUtil.equalsClassNames(subjectClass.getFQN(), expectedClass.getFQN())) {
            return true;
        }

        Ref<Boolean> ref = new Ref<>(false);
        PhpClassHierarchyUtils.processSupers(subjectClass, false, true, curClass -> {
            ref.set(PhpLangUtil.equalsClassNames(curClass.getFQN(), expectedClass.getFQN()));
            return !(Boolean) ref.get();
        });

        return ref.get();
    }

    /**
     * @param subjectClass eg DateTime
     * @param expectedClassAsString eg DateTimeInterface
     */
    public static boolean isInstanceOf(@NotNull PhpClass subjectClass, @NotNull String expectedClassAsString) {
        if (("\\" + StringUtils.stripStart(expectedClassAsString, "\\")).equals(subjectClass.getFQN())) {
            return true;
        }

        for (PhpClass expectedClass : PhpIndex.getInstance(subjectClass.getProject()).getAnyByFQN(expectedClassAsString)) {
            if (isInstanceOf(subjectClass, expectedClass)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isInstanceOf(@NotNull PhpClass subjectClass, @NotNull String ...expectedClassAsString) {
        return Arrays.stream(expectedClassAsString).anyMatch(clazz -> isInstanceOf(subjectClass, clazz));
    }

    /**
     * @param subjectClassAsString eg DateTime
     * @param expectedClass eg DateTimeInterface
     */
    public static boolean isInstanceOf(@NotNull Project project, @NotNull String subjectClassAsString, @NotNull String expectedClass) {
        if (("\\" + StringUtils.stripStart(subjectClassAsString, "\\")).equals(("\\" + StringUtils.stripStart(expectedClass, "\\")))) {
            return true;
        }

        for (PhpClass subjectClass : PhpIndex.getInstance(project).getAnyByFQN(subjectClassAsString)) {
            if (isInstanceOf(subjectClass, expectedClass)) {
                return true;
            }
        }

        return false;
    }

    static public Collection<PhpClass> getClassesInterface(Project project, @NotNull String className) {
        return PhpIndex.getInstance(project).getAnyByFQN(className);
    }

    static public void addClassPublicMethodCompletion(CompletionResultSet completionResultSet, PhpClass phpClass) {
        for(Method method: getClassPublicMethod(phpClass)) {
            completionResultSet.addElement(new PhpLookupElement(method));
        }
    }

    static public ArrayList<Method> getClassPublicMethod(PhpClass phpClass) {
        ArrayList<Method> methods = new ArrayList<>();

        for(Method method: phpClass.getMethods()) {
            if(method.getAccess().isPublic() && !method.getName().startsWith("__")) {
                methods.add(method);
            }
        }

        return methods;
    }

    static public boolean hasClassConstantFields(@NotNull PhpClass phpClass) {
        return phpClass.getFields().stream().anyMatch(Field::isConstant);
    }

    @Nullable
    static public String getArrayHashValue(ArrayCreationExpression arrayCreationExpression, String keyName) {
        ArrayHashElement translationArrayHashElement = PsiElementUtils.getChildrenOfType(arrayCreationExpression, PlatformPatterns.psiElement(ArrayHashElement.class)
            .withFirstChild(
                PlatformPatterns.psiElement(PhpElementTypes.ARRAY_KEY).withText(
                    PlatformPatterns.string().oneOf("'" + keyName + "'", "\"" + keyName + "\"")
                )
            )
        );

        if(translationArrayHashElement == null) {
            return null;
        }

        if(!(translationArrayHashElement.getValue() instanceof StringLiteralExpression valueString)) {
            return null;
        }

        return valueString.getContents();
    }

    static public boolean isEqualMethodReferenceName(MethodReference methodReference, String methodName) {
        String name = methodReference.getName();
        return name != null && name.equals(methodName);
    }

    @Nullable
    static public String getArrayKeyValueInsideSignature(PsiElement psiElementInsideClass, String[] insideMethods, String methodName, String keyName) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElementInsideClass, PhpClass.class);
        if (phpClass == null) {
            return null;
        }

        for (String insideMethod : insideMethods) {
            Method method = phpClass.findMethodByName(insideMethod);
            if (method == null) {
                continue;
            }

            for (MethodReference methodReference : collectMethodReferencesInsideControlFlow(method, methodName)) {
                PsiElement[] parameters = methodReference.getParameters();
                if (parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                    PhpPsiElement arrayValue = PhpElementsUtil.getArrayValue((ArrayCreationExpression) parameters[0], keyName);
                    if (arrayValue != null) {
                        String stringValue = getStringValue(arrayValue);
                        if (stringValue != null && !stringValue.isBlank()) {
                            return stringValue;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static Method[] getImplementedMethods(@NotNull Method method) {
        ArrayList<Method> items = getImplementedMethods(method.getContainingClass(), method, new ArrayList<>(), new HashSet<>());
        return items.toArray(new Method[0]);
    }

    private static ArrayList<Method> getImplementedMethods(@Nullable PhpClass phpClass, @NotNull Method method, ArrayList<Method> implementedMethods, Set<PhpClass> visitedClasses) {
        if (phpClass == null || visitedClasses.contains(phpClass)) {
            return implementedMethods;
        }

        visitedClasses.add(phpClass);

        Method[] methods = phpClass.getOwnMethods();
        for (Method ownMethod : methods) {
            if (PhpLangUtil.equalsMethodNames(ownMethod.getName(), method.getName())) {
                implementedMethods.add(ownMethod);
            }
        }

        for(PhpClass interfaceClass: phpClass.getImplementedInterfaces()) {
            getImplementedMethods(interfaceClass, method, implementedMethods, visitedClasses);
        }

        getImplementedMethods(phpClass.getSuperClass(), method, implementedMethods, visitedClasses);

        return implementedMethods;
    }

    /**
     * Resolve string definition in a recursive way
     *
     * $foo = Foo::class
     * $this->foo = Foo::class
     * $this->foo1 = $this->foo
     */
    @Nullable
    public static String getStringValue(@Nullable PsiElement psiElement) {
        return getStringValue(psiElement, 0);
    }

    @Nullable
    private static String getStringValue(@Nullable PsiElement psiElement, int depth) {
        if(psiElement == null || ++depth > 5) {
            return null;
        }

        if(psiElement instanceof StringLiteralExpression) {
            String resolvedString = ((StringLiteralExpression) psiElement).getContents();
            if(StringUtils.isEmpty(resolvedString)) {
                return null;
            }

            return resolvedString;
        } else if(psiElement instanceof Field) {
            return getStringValue(((Field) psiElement).getDefaultValue(), depth);
        } else if(psiElement instanceof ClassConstantReference && "class".equals(((ClassConstantReference) psiElement).getName())) {
            // Foobar::class
            return getClassConstantPhpFqn((ClassConstantReference) psiElement);
        } else if(psiElement instanceof PhpReference) {
            PsiReference psiReference = psiElement.getReference();
            if(psiReference == null) {
                return null;
            }

            PsiElement ref = psiReference.resolve();
            if(ref instanceof PhpReference) {
                return getStringValue(psiElement, depth);
            }

            if(ref instanceof Field) {
                return getStringValue(((Field) ref).getDefaultValue());
            }
        }

        return null;
    }

    public static String getPrevSiblingAsTextUntil(PsiElement psiElement, ElementPattern pattern, boolean includeMatching) {
        StringBuilder prevText = new StringBuilder();

        for (PsiElement child = psiElement.getPrevSibling(); child != null; child = child.getPrevSibling()) {
            if(pattern.accepts(child)) {
                if(includeMatching) {
                    return child.getText() + prevText;
                }
                return prevText.toString();
            } else {
                prevText.insert(0, child.getText());
            }
        }

        return prevText.toString();
    }

    public static String getPrevSiblingAsTextUntil(PsiElement psiElement, ElementPattern pattern) {
        return getPrevSiblingAsTextUntil(psiElement, pattern, false);
    }

    @Nullable
    public static ArrayCreationExpression getCompletableArrayCreationElement(PsiElement psiElement) {

        // array('<test>' => '')
        if(PhpPatterns.psiElement(PhpElementTypes.ARRAY_KEY).accepts(psiElement.getContext())) {
            PsiElement arrayKey = psiElement.getContext();
            if(arrayKey != null) {
                PsiElement arrayHashElement = arrayKey.getContext();
                if(arrayHashElement instanceof ArrayHashElement) {
                    PsiElement arrayCreationExpression = arrayHashElement.getContext();
                    if(arrayCreationExpression instanceof ArrayCreationExpression) {
                        return (ArrayCreationExpression) arrayCreationExpression;
                    }
                }
            }

        }

        // on array creation key dont have value, so provide completion here also
        // array('foo' => 'bar', '<test>')
        if(PhpPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).accepts(psiElement.getContext())) {
            PsiElement arrayKey = psiElement.getContext();
            if(arrayKey != null) {
                PsiElement arrayCreationExpression = arrayKey.getContext();
                if(arrayCreationExpression instanceof ArrayCreationExpression) {
                    return (ArrayCreationExpression) arrayCreationExpression;
                }

            }

        }

        return null;
    }

    static public PsiElementPattern.Capture<StringLiteralExpression> getPar() {
        return PlatformPatterns
            .psiElement(StringLiteralExpression.class)
            .withParent(
                PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
                    .withFirstChild(
                        PlatformPatterns.psiElement(PhpElementTypes.STRING)
                    )
                    .withParent(
                        PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE)
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE);
    }

    public static Collection<PhpClass> getClassFromPhpTypeSet(Project project, Set<String> types) {

        PhpType phpType = new PhpType();
        for (String type : types) {
            phpType.add(type);
        }

        List<PhpClass> phpClasses = new ArrayList<>();

        for(String typeName: PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<>()).getTypes()) {
            if(typeName.startsWith("\\")) {
                PhpClass phpClass = PhpElementsUtil.getClassInterface(project, typeName);
                if(phpClass != null) {
                    phpClasses.add(phpClass);
                }
            }
        }

        return phpClasses;
    }

    public static Collection<PhpClass> getClassFromPhpTypeSetArrayClean(Project project, Set<String> types) {

        PhpType phpType = new PhpType();
        for (String type : types) {
            phpType.add(type);
        }

        ArrayList<PhpClass> phpClasses = new ArrayList<>();

        for(String typeName: PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<>()).getTypes()) {
            if(typeName.startsWith("\\")) {

                // we clean array types \Foo[]
                if(typeName.endsWith("[]")) {
                    typeName = typeName.substring(0, typeName.length() - 2);
                }

                PhpClass phpClass = PhpElementsUtil.getClassInterface(project, typeName);
                if(phpClass != null) {
                    phpClasses.add(phpClass);
                }
            }
        }

        return phpClasses;
    }

    @Nullable
    public static PhpClass getFirstClassFromFile(PhpFile phpFile) {
        for (PhpNamedElement topLevelElement : phpFile.getTopLevelDefs().values()) {
            if (topLevelElement instanceof PhpClass clazz) {
                return clazz;
            }
        }

        return null;
    }

    public static boolean isEqualClassName(@Nullable PhpClass phpClass, @Nullable String... compareClassNames) {

        for(String className: compareClassNames) {
            if(isEqualClassName(phpClass, className)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isEqualClassName(@NotNull PhpClass phpClass, @NotNull PhpClass compareClassName) {
        return isEqualClassName(phpClass, compareClassName.getPresentableFQN());
    }

    public static boolean isEqualClassName(@Nullable PhpClass phpClass, @Nullable String compareClassName) {
        if(phpClass == null || compareClassName == null) {
            return false;
        }

        return StringUtils.stripStart(phpClass.getPresentableFQN(), "\\")
            .equals(StringUtils.stripStart(compareClassName, "\\"));
    }

    public static boolean isEqualClassName(@NotNull String phpClass, @NotNull String ...compareClassNames) {
        for (String compareClassName : compareClassNames) {
            if (Objects.equals(StringUtils.stripStart(phpClass, "\\"), StringUtils.stripStart(compareClassName, "\\"))) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static PsiElement[] getMethodParameterReferences(@NotNull Method method, int parameterIndex) {
        // we dont have a parameter on resolved method
        Parameter[] parameters = method.getParameters();

        if(parameters.length == 0 || parameterIndex >= parameters.length) {
            return new PsiElement[0];
        }

        String tempVariableName = parameters[parameterIndex].getName();

        Collection<PsiElement> psiElements = new ArrayList<>();

        PhpControlFlowUtil.processFlow(method.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processAccessVariableInstruction(PhpAccessVariableInstruction instruction) {
                if (tempVariableName.contentEquals(instruction.getVariableName())) {
                    psiElements.add(instruction.getAnchor());
                }

                return super.processAccessVariableInstruction(instruction);
            }
        });

        return psiElements.toArray(new PsiElement[0]);
    }

    @Nullable
    public static MethodReferenceBag getMethodParameterReferenceBag(PsiElement psiElement, int wantIndex) {

        PsiElement variableContext = psiElement.getContext();
        if(!(variableContext instanceof ParameterList parameterList)) {
            return null;
        }

        if (!(parameterList.getContext() instanceof MethodReference)) {
            return null;
        }

        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
        if(currentIndex == null) {
            return null;
        }

        if(wantIndex >= 0 && currentIndex.getIndex() != wantIndex) {
            return null;
        }

        return new MethodReferenceBag(parameterList, (MethodReference) parameterList.getContext(), currentIndex);

    }

    @Nullable
    public static MethodReferenceBag getMethodParameterReferenceBag(PsiElement psiElement) {
        return getMethodParameterReferenceBag(psiElement, -1);
    }

    /**
     * Find all variables in current function / method scope
     *
     * $v<caret>ar = 'foobar';
     * $v<caret>ar->foo()
     */
    public static Collection<Variable> getVariableReferencesInScope(@NotNull Variable variable) {
        Variable variableDecl = null;
        if(!variable.isDeclaration()) {
            PsiElement psiElement = variable.resolve();
            if(psiElement instanceof Variable) {
                variableDecl = (Variable) psiElement;
            }
        } else {
            variableDecl = variable;
        }

        if(variableDecl == null) {
            return Collections.emptyList();
        }

        Function function = PsiTreeUtil.getParentOfType(variable, Function.class);
        if(function == null) {
            return Collections.emptyList();
        }

        final List<Variable> variables = new ArrayList<>();

        for (Variable variableRef : PhpElementsUtil.getVariablesInScopeByName(function, variableDecl.getName())) {
            if (!variableRef.equals(variable)) {
                variables.add(variableRef);
            }
        }

        return variables;
    }

    /**
     * Try to visit possible class name for PsiElements with text like "Foo\|Bar", "Foo|\Bar", "\Foo|\Bar"
     * Cursor must have position in PsiElement
     *
     * @param psiElement the element context, cursor should be in it
     * @param cursorOffset current cursor editor eg from completion context
     * @param visitor callback on matching class
     */
    public static void visitNamespaceClassForCompletion(PsiElement psiElement, int cursorOffset, ClassForCompletionVisitor visitor) {

        int cursorOffsetClean = cursorOffset - psiElement.getTextOffset();
        if(cursorOffsetClean < 1) {
            return;
        }

        String content = psiElement.getText();
        int length = content.length();
        if(!(length >= cursorOffsetClean)) {
            return;
        }

        String beforeCursor = content.substring(0, cursorOffsetClean);
        boolean isValid;

        // espend\|Container, espend\Cont|ainer <- fallback to last full namespace
        // espend|\Container <- only on known namespace "espend"
        String namespace = beforeCursor;

        // if no backslash or its equal in first position, fallback on namespace completion
        int lastSlash = beforeCursor.lastIndexOf("\\");
        if(lastSlash <= 0) {
            isValid = PhpIndexUtil.hasNamespace(psiElement.getProject(), beforeCursor);
        } else {
            isValid = true;
            namespace = beforeCursor.substring(0, lastSlash);
        }

        if(!isValid) {
            return;
        }

        // format namespaces and add prefix for fluent completion
        String prefix = "";
        if(namespace.startsWith("\\")) {
            prefix = "\\";
        } else {
            namespace = "\\" + namespace;
        }

        // search classes in current namespace and child namespaces
        for(PhpClass phpClass: PhpIndexUtil.getPhpClassInsideNamespace(psiElement.getProject(), namespace)) {
            String presentableFQN = phpClass.getPresentableFQN();
            if(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.startWithEqualClassname(presentableFQN, beforeCursor)) {
                visitor.visit(phpClass, presentableFQN, prefix);
            }

        }

    }

    public interface ClassForCompletionVisitor {
        void visit(PhpClass phpClass, String presentableFQN, String prefix);
    }

    /**
     * new FooClass()
     */
    @Nullable
    @Deprecated
    public static PhpClass getNewExpressionPhpClass(@NotNull NewExpression newExpression) {
        Collection<PhpClass> newExpressionPhpClasses = getNewExpressionPhpClasses(newExpression);
        if (!newExpressionPhpClasses.isEmpty()) {
            return newExpressionPhpClasses.iterator().next();
        }

        return null;
    }

    @NotNull
    public static Collection<PhpClass> getNewExpressionPhpClasses(@NotNull NewExpression newExpression) {
        PhpIndex instance = PhpIndex.getInstance(newExpression.getProject());
        PhpType classType = (new PhpType()).add(newExpression.getClassReference()).global(newExpression.getProject());

        return classType.getTypes()
            .stream()
            .flatMap((fqn) -> instance.getAnyByFQN(fqn).stream())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * Get PhpClass from "new FooClass()" only if match instance condition
     */
    public static PhpClass getNewExpressionPhpClassWithInstance(@NotNull NewExpression newExpression, @NotNull String instance) {
        for (PhpClass phpClass : getNewExpressionPhpClasses(newExpression)) {
            if (PhpElementsUtil.isInstanceOf(phpClass, instance)) {
                return phpClass;
            }
        }

        return null;
    }

    /**
     * Check new class is instance of given type eg: "new \Twig_Test()"
     */
    public static boolean isNewExpressionPhpClassWithInstance(@NotNull NewExpression newExpression, @NotNull String... instances) {
        for (String instance : instances) {
            PhpClass phpClass = getNewExpressionPhpClassWithInstance(newExpression, instance);
            if (phpClass != null) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static Collection<PsiElement> collectMethodElementsWithParents(final @NotNull Method method, @NotNull final Processor<PsiElement> processor) {
        Collection<PsiElement> elements = new HashSet<>();
        collectMethodElementsWithParents(method, 3, elements, processor);
        return elements;
    }

    private static void collectMethodElementsWithParents(final @NotNull Method method, final int depth, @NotNull final Collection<PsiElement> elements, @NotNull final Processor<PsiElement> processor) {

        method.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {

                if (processor.process(psiElement)) {
                    elements.add(psiElement);
                }

                if (psiElement instanceof MethodReference && ((MethodReference) psiElement).getReferenceType() == PhpModifier.State.PARENT && method.getName().equals(((MethodReference) psiElement).getName())) {
                    PsiElement resolve = ((MethodReference) psiElement).resolve();
                    if (depth > 0 && resolve instanceof Method) {
                        collectMethodElementsWithParents((Method) resolve, depth - 1, elements, processor);
                    }
                }

                super.visitElement(psiElement);
            }
        });

    }

    /**
     * Gets parameter which are non optional and at the end of a function signature
     *
     * foo($container, $bar = null, $foo = null);
     *
     */
    @NotNull
    public static Parameter[] getFunctionRequiredParameter(@NotNull Function function) {

        // nothing we need to do
        Parameter[] parameters = function.getParameters();
        if(parameters.length == 0) {
            return new Parameter[0];
        }

        // find last optional parameter
        int last = -1;
        for (int i = parameters.length - 1; i >= 0; i--) {
            if(!parameters[i].isOptional()) {
                last = i;
                break;
            }
        }

        // no required argument found
        if(last == -1) {
            return new Parameter[0];
        }

        return Arrays.copyOfRange(parameters, 0, last + 1);
    }

    public static boolean isTestClass(@NotNull PhpClass phpClass) {

        if(PhpUnitUtil.isTestClass(phpClass)) {
            return true;
        }

        String fqn = phpClass.getPresentableFQN();
        return fqn.contains("\\Test\\") || fqn.contains("\\Tests\\");
    }

    /**
     * Extract type hint from method parameter
     *
     * function foo(\FooClass $class)
     */
    @NotNull
    public static Collection<String> getMethodParameterTypeHints(@NotNull Method method) {
        ParameterList childOfType = PsiTreeUtil.getChildOfType(method, ParameterList.class);
        if(childOfType == null) {
            return Collections.emptyList();
        }

        PsiElement[] parameters = childOfType.getParameters();
        if(parameters.length == 0) {
            return Collections.emptyList();
        }
        
        PhpTypeDeclaration typeDeclaration = PsiTreeUtil.getChildOfType(parameters[0], PhpTypeDeclaration.class);
        if (typeDeclaration == null) return Collections.emptyList();
        return typeDeclaration.getClassReferences().stream()
            .map(PhpReference::getFQN)
            .collect(Collectors.toList());
    }

    /**
     * Find first variable declaration in parent scope of a given variable:
     *
     * function() {
     *   $event = new FooEvent();
     *   dispatch('foo', $event);
     * }
     */
    @Nullable
    public static String getFirstVariableTypeInScope(@NotNull Variable variable) {
        // parent search scope, eg Method else fallback to a grouped statement
        PhpScopeHolder searchScope = PsiTreeUtil.getParentOfType(variable, PhpScopeHolder.class);
        if(searchScope == null) {
            return null;
        }

        final String name = variable.getName();
        final String[] result = {null};

        PhpControlFlowUtil.processFlow(searchScope.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processAccessVariableInstruction(PhpAccessVariableInstruction instruction) {
                PhpPsiElement element = instruction.getAnchor();
                if (name.contentEquals(instruction.getVariableName()) && element instanceof Variable) {
                    PsiElement assignmentExpression = element.getParent();
                    if(assignmentExpression instanceof AssignmentExpression) {
                        PhpPsiElement value = ((AssignmentExpression) assignmentExpression).getValue();
                        if(value instanceof NewExpression) {
                            ClassReference classReference = ((NewExpression) value).getClassReference();
                            if(classReference != null) {
                                String classSignature = classReference.getFQN();
                                if(StringUtils.isNotBlank(classSignature)) {
                                    result[0] = classSignature;
                                }
                            }
                        }
                    }
                }

                return super.processAccessVariableInstruction(instruction);
            }
        });

        return result[0];
    }

    /**
     * Get class by shortcut namespace, on a scoped namespace

     * @param project current project
     * @param classNameScope Namespace fo search "\Foo\Foo", "Foo\Foo", "Foo\Foo\", last "\*" is stripped
     * @param className Class name inside namespace also fqn is supported

     * @return PhpClass matched
     */
    public static PhpClass getClassInsideNamespaceScope(@NotNull Project project, @NotNull String classNameScope, @NotNull String className) {

        if(className.startsWith("\\")) {
            return PhpElementsUtil.getClassInterface(project, className);
        }

        // strip class name we namespace
        String strip = StringUtils.strip(classNameScope, "\\");
        int i = strip.lastIndexOf("\\");
        if(i <= 0) {
            return PhpElementsUtil.getClassInterface(project, className);
        }

        PhpClass phpClass = PhpElementsUtil.getClassInterface(project, strip.substring(0, i) + "\\" + StringUtils.strip(className, "\\"));
        if(phpClass != null) {
            return phpClass;
        }

        return PhpElementsUtil.getClassInterface(project, className);
    }

    /**
     * Resolves MethodReference and compare containing class against implementations instances
     */
    public static boolean isMethodReferenceInstanceOf(@NotNull MethodReference methodReference, @NotNull String expectedClassName) {
        PhpIndex instance = PhpIndex.getInstance(methodReference.getProject());
        PhpType classType = (new PhpType()).add(methodReference.getClassReference()).global(methodReference.getProject());

        Collection<PhpClass> instanceClasses = classType.getTypes()
            .stream()
            .flatMap((fqn) -> instance.getAnyByFQN(fqn).stream())
            .distinct()
            .toList();

        for (PhpClass phpClass : instanceClasses) {
            Method method = phpClass.findMethodByName(methodReference.getName());
            if (method == null) {
                continue;
            }

            // different class possible
            PhpClass containingClass = method.getContainingClass();
            if(containingClass == null) {
                continue;
            }

            if(!PhpElementsUtil.isInstanceOf(containingClass, expectedClassName)) {
                continue;
            }

            return true;
        }

        return false;
    }

    /**
     * Resolves MethodReference and compare containing class against implementations instances
     */
    public static boolean isMethodReferenceInstanceOf(@NotNull MethodReference methodReference, @NotNull String expectedClassName, @NotNull String methodName) {
        if(!methodName.equals(methodReference.getName())) {
            return false;
        }

        return isMethodReferenceInstanceOf(methodReference, expectedClassName);
    }


    /**
     * Try to find method matching on any "className::method" giving
     */
    public static boolean isMethodReferenceInstanceOf(@NotNull MethodReference methodReference, @NotNull MethodMatcher.CallToSignature... signatures) {
        for (MethodMatcher.CallToSignature method : signatures) {
            if (isMethodReferenceInstanceOf(methodReference, method.getInstance(), method.getMethod())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMethodInstanceOf(@NotNull Method method, @NotNull String clazz, @NotNull String methodName) {
        return isMethodInstanceOf(method, new MethodMatcher.CallToSignature(clazz, methodName));
    }

    public static boolean isMethodInstanceOf(Method method, @NotNull MethodMatcher.CallToSignature... signatures) {
        PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return false;
        }

        for (MethodMatcher.CallToSignature signature : signatures) {
            String methodName = signature.getMethod();

            if(!methodName.equals(method.getName())) {
                continue;
            }

            if (PhpElementsUtil.isInstanceOf(containingClass, signature.getInstance())) {
                return true;
            }
        }

        return false;
    }

    public static void replaceElementWithClassConstant(@NotNull PhpClass phpClass, @NotNull PsiElement originElement) throws Exception{
        String fqn = phpClass.getFQN();
        if(!fqn.startsWith("\\")) {
            fqn = "\\" + fqn;
        }

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(originElement);
        if(scopeForUseOperator == null) {
            throw new Exception("Class fqn error");
        }

        if(!PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).containsValue(fqn)) {
            PhpAliasImporter.insertUseStatement(fqn, scopeForUseOperator);
        }

        originElement.replace(PhpPsiElementFactory.createPhpPsiFromText(
            originElement.getProject(),
            ClassConstantReference.class,
            "<?php " + phpClass.getName() + "::class"
        ));
    }

    /**
     * add('', <caret>), add('', Foo<caret>)
     */
    @Nullable
    public static MethodReference findMethodReferenceOnClassConstant(PsiElement psiElement) {
        PsiElement parameterList = psiElement.getParent();
        if(parameterList instanceof ParameterList) {
            PsiElement psiElement2 = parameterList.getParent();
            if(psiElement2 instanceof MethodReference) {
                return (MethodReference) psiElement2;
            }
        } else if(parameterList instanceof ConstantReference) {
            PsiElement parent = parameterList.getParent();
            if(parent instanceof ParameterList) {
                return PsiElementAssertUtil.getParentOfTypeOrNull(parent, MethodReference.class);
            }
        }

        return null;
    }

    /**
     * Foo::class to its PhpClass
     */
    public static PhpClass getClassConstantPhpClass(@NotNull ClassConstantReference classConstant) {
        String typeName = getClassConstantPhpFqn(classConstant);
        return typeName != null ? PhpElementsUtil.getClassInterface(classConstant.getProject(), typeName) : null;
    }

    /**
     * Foo::class to its class fqn include namespace
     */
    public static String getClassConstantPhpFqn(@NotNull ClassConstantReference classConstant) {
        PhpExpression classReference = classConstant.getClassReference();
        if(!(classReference instanceof PhpReference)) {
            return null;
        }

        String typeName = ((PhpReference) classReference).getFQN();
        return typeName != null && StringUtils.isNotBlank(typeName) ? StringUtils.stripStart(typeName, "\\") : null;
    }

    /**
     * Get type hint PhpClass of an given method index
     *
     * @param parameterIndex staring 0
     */
    @Nullable
    public static PhpClass getMethodTypeHintParameterPhpClass(@NotNull Method method, int parameterIndex) {
        Parameter[] constructorParameter = method.getParameters();
        if(parameterIndex >= constructorParameter.length) {
            return null;
        }

        String className = constructorParameter[parameterIndex].getDeclaredType().toString();
        if(StringUtils.isBlank(className)) {
            return null;
        }

        return PhpElementsUtil.getClassInterface(method.getProject(), className);
    }

    @Nullable
    public static String insertUseIfNecessary(@NotNull PsiElement phpClass, @NotNull String fqnClasName) {
        if(!fqnClasName.startsWith("\\")) {
            fqnClasName = "\\" + fqnClasName;
        }

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(phpClass);
        if(scopeForUseOperator == null) {
            return null;
        }

        if(!PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).containsValue(fqnClasName)) {
            PhpAliasImporter.insertUseStatement(fqnClasName, scopeForUseOperator);
        }

        for (Map.Entry<String, String> entry : PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).entrySet()) {
            if(fqnClasName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * #[AsEventListener(method:'onBarEvent')]
     */
    public static boolean isAttributeNamedArgumentString(@NotNull StringLiteralExpression element, @NotNull String fqn, @NotNull String namedArgument) {
        PsiElement colon = PsiTreeUtil.prevCodeLeaf(element);
        if (colon == null || colon.getNode().getElementType() != PhpTokenTypes.opCOLON) {
            return false;
        }

        PsiElement argumentName = PsiTreeUtil.prevCodeLeaf(colon);
        if (argumentName == null || argumentName.getNode().getElementType() != PhpTokenTypes.IDENTIFIER || !namedArgument.equals(argumentName.getText())) {
            return false;
        }


        if (element.getParent() instanceof ParameterList parameterList) {
            if (parameterList.getParent() instanceof PhpAttribute phpAttribute) {
                String attributeFqn = phpAttribute.getFQN();
                return fqn.equals(attributeFqn);
            }
        }

        return false;
    }

    /**
     * Collects all variables in a given scope.
     * Eg find all variables usages in a given method
     */
    @NotNull
    public static Set<Variable> getVariablesInScope(@NotNull PsiElement psiElement, @NotNull PhpNamedElement variable) {
        return MyVariableRecursiveElementVisitor.visit(psiElement, variable.getName());
    }

    public static Set<Variable> getVariablesInScopeByName(@NotNull PhpScopeHolder phpScopeHolder, @NotNull String name) {
        Set<Variable> variables = new HashSet<>();

        PhpControlFlowUtil.processFlow(phpScopeHolder.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processAccessVariableInstruction(PhpAccessVariableInstruction instruction) {
                if (name.contentEquals(instruction.getVariableName()) && instruction.getAnchor() instanceof Variable variable) {
                    variables.add(variable);
                }

                return super.processAccessVariableInstruction(instruction);
            }
        });

        return variables;
    }

    /**
     * Provide array key pattern. we need incomplete array key support, too.
     *
     * foo(['<caret>'])
     * foo(['<caret>' => 'foobar'])
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> getParameterListArrayValuePattern() {
        return PlatformPatterns.psiElement()
            .withParent(PlatformPatterns.psiElement(StringLiteralExpression.class).withParent(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement().withElementType(PhpElementTypes.ARRAY_VALUE)
                        .withParent(PlatformPatterns.psiElement(ArrayCreationExpression.class)
                            .withParent(ParameterList.class)
                        ),

                    PlatformPatterns.psiElement().withElementType(PhpElementTypes.ARRAY_KEY)
                        .withParent(PlatformPatterns.psiElement(ArrayHashElement.class)
                            .withParent(PlatformPatterns.psiElement(ArrayCreationExpression.class)
                                .withParent(ParameterList.class)
                            )
                        )
                ))
            );
    }

    /**
     * true ? "foo" : "Foobar::class"
     */
    @NotNull
    public static Collection<String> getTernaryExpressionConditionStrings(@NotNull TernaryExpression firstPsiChild) {
        Collection<String> types = new ArrayList<>();

        PsiElement[] psiElements = {
            firstPsiChild.getFalseVariant(),
            firstPsiChild.getTrueVariant()
        };

        for (PsiElement psiElement : psiElements) {
            if(psiElement != null) {
                String stringValue = PhpElementsUtil.getStringValue(psiElement);
                if(stringValue != null) {
                    types.add(stringValue);
                }
            }
        }

        return types;
    }

    /**
     * Find argument by name in constructor parameter: __construct($foobar)
     */
    @Nullable
    public static Parameter getConstructorParameterArgumentByName(@NotNull PhpClass phpClass, @NotNull String argumentName) {
        Method constructor = phpClass.getConstructor();
        if(constructor == null) {
            return null;
        }

        return Arrays.stream(constructor.getParameters()).filter(
                parameter -> argumentName.equals(parameter.getName())
        ).findFirst().orElse(null);
    }

    /**
     * Find argument by name in constructor parameter: __construct($foobar)
     */
    public static int getConstructorArgumentByName(@NotNull PhpClass phpClass, @NotNull String argumentName) {
        Method constructor = phpClass.getConstructor();
        if(constructor == null) {
            return -1;
        }

        return getFunctionArgumentByName(constructor, argumentName);
    }

    /**
     * Find argument by name in function parameter: argumentName($foobar)
     */
    public static int getFunctionArgumentByName(@NotNull Function function, @NotNull String argumentName) {
        Parameter[] parameters = function.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (argumentName.equals(parameter.getName())) {
                return i;
            }
        }

        return -1;
    }

    @NotNull
    public static Collection<Method> getMultiResolvedMethod(@NotNull MethodReference methodReference) {
        PhpIndex instance = PhpIndex.getInstance(methodReference.getProject());
        PhpType classType = (new PhpType()).add(methodReference.getClassReference()).global(methodReference.getProject());

        Collection<PhpClass> instanceClasses = classType.getTypes()
            .stream()
            .flatMap((fqn) -> instance.getAnyByFQN(fqn).stream())
            .distinct()
            .toList();

        Set<Method> methods = new HashSet<>();
        for (PhpClass phpClass : instanceClasses) {
            Method method = phpClass.findMethodByName(methodReference.getName());
            if (method != null) {
                methods.add(method);
            }
        }

        return methods;
    }

    /**
     * Get first string value of MethodReference; Not index access allowed!
     * see getMethodReferenceStringValueParameter for resolving value in detail
     */
    @Nullable
    public static String getFirstArgumentStringValue(@NotNull MethodReference methodReference) {
        String stringValue = null;

        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length > 0 && parameters[0] instanceof StringLiteralExpression) {
            stringValue = ((StringLiteralExpression) parameters[0]).getContents();
        }

        return stringValue;
    }

    /**
     * Get resolved string value
     *
     * $f->foo('index_0', 'index_1');
     */
    @Nullable
    public static String getMethodReferenceStringValueParameter(@NotNull MethodReference methodReference, int parameter) {
        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length > parameter) {
            return getStringValue(parameters[parameter]);
        }

        return null;
    }

    @NotNull
    public static Collection<NewExpression> collectNewExpressionsInsideControlFlow(@NotNull PhpScopeHolder phpScopeHolder) {
        Collection<NewExpression> newExpressions = new ArrayList<>();

        PhpControlFlowUtil.processFlow(phpScopeHolder.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processConstructorCallInstruction(PhpConstructorCallInstruction instruction) {
                newExpressions.add(instruction.getNewExpression());
                return super.processConstructorCallInstruction(instruction);
            }
        });

        return newExpressions;
    }

    public static Collection<PsiElement> collectPhpReturnArgumentsInsideControlFlow(@NotNull PhpScopeHolder phpScopeHolder) {
        Collection<PsiElement> elements = new ArrayList<>();

        PhpControlFlowUtil.processFlow(phpScopeHolder.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processReturnInstruction(PhpReturnInstruction instruction) {
                PsiElement argument = instruction.getArgument();
                if (argument != null) {
                    elements.add(argument);
                }

                return super.processReturnInstruction(instruction);
            }
        });

        return elements;
    }

    public static Collection<MethodReference> collectMethodReferencesInsideControlFlow(@NotNull PhpScopeHolder phpScopeHolder) {
        Collection<MethodReference> methodReferences = new ArrayList<>();

        PhpControlFlowUtil.processFlow(phpScopeHolder.getControlFlow(), new PhpInstructionProcessor() {
            public boolean processPhpCallInstruction(PhpCallInstruction instruction) {
                if (instruction.getFunctionReference() instanceof MethodReference methodReference) {
                    methodReferences.add(methodReference);
                }
                return super.processPhpCallInstruction(instruction);
            }
        });

        return methodReferences;
    }

    public static Collection<MethodReference> collectMethodReferencesInsideControlFlow(@NotNull PhpScopeHolder phpScopeHolder, @NotNull String... methodName) {
        Collection<MethodReference> methodReferences = new ArrayList<>();

        PhpControlFlowUtil.processFlow(phpScopeHolder.getControlFlow(), new PhpInstructionProcessor() {
            public boolean processPhpCallInstruction(PhpCallInstruction instruction) {
                if (instruction.getFunctionReference() instanceof MethodReference methodReference && Arrays.stream(methodName).anyMatch(s -> s.equals(methodReference.getName()))) {
                    methodReferences.add(methodReference);
                }
                return super.processPhpCallInstruction(instruction);
            }
        });

        return methodReferences;
    }

    @NotNull
    public static Collection<Function> getMethodReferenceMethods(@NotNull MethodReference methodReference) {
        PhpIndex instance = PhpIndex.getInstance(methodReference.getProject());
        PhpType classType = (new PhpType()).add(methodReference.getClassReference()).global(methodReference.getProject());

        Set<PhpClass> collect = classType.getTypes()
            .stream()
            .flatMap((fqn) -> instance.getAnyByFQN(fqn).stream())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return collect
            .stream()
            .map((java.util.function.Function<PhpClass, Function>) phpClass -> phpClass.findMethodByName(methodReference.getName()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public static PhpExpectedFunctionArgument findAttributeArgumentByName(@NotNull String attributeName, @NotNull PhpAttribute phpAttribute) {
        for (PhpAttribute.PhpAttributeArgument argument : phpAttribute.getArguments()) {
            String name = argument.getName();
            if (!attributeName.equals(name)) {
                continue;
            }

            return argument.getArgument();
        }

        return null;
    }

    @Nullable
    public static String getAttributeArgumentStringByName(@NotNull PhpAttribute phpAttribute, @NotNull String attributeName) {
        for (PhpAttribute.PhpAttributeArgument argument : phpAttribute.getArguments()) {
            if (!attributeName.equals(argument.getName())) {
                continue;
            }

            if (argument.getArgument() instanceof PhpExpectedFunctionClassConstantArgument phpExpectedFunctionClassConstantArgument) {
                return phpExpectedFunctionClassConstantArgument.getClassFqn();
            }

            if (argument.getArgument() instanceof PhpExpectedFunctionScalarArgument phpExpectedFunctionScalarArgument) {
                return PsiElementUtils.trimQuote(phpExpectedFunctionScalarArgument.getNormalizedValue());
            }
        }

        return null;
    }

    @Nullable
    public static String findAttributeArgumentByNameAsString(@NotNull String attributeName, @NotNull PhpAttribute phpAttribute) {
        PhpExpectedFunctionArgument attributeArgumentByName = findAttributeArgumentByName(attributeName, phpAttribute);
        if (attributeArgumentByName instanceof PhpExpectedFunctionScalarArgument) {
            String value = PsiElementUtils.trimQuote(attributeArgumentByName.getValue());
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }

        return null;
    }
    /**
     * Visit and collect all variables in given scope
     */
    private static class MyVariableRecursiveElementVisitor extends PsiRecursiveElementVisitor {
        @NotNull
        private final String name;

        @NotNull
        private final Set<Variable> variables = new HashSet<>();

        MyVariableRecursiveElementVisitor(@NotNull String name) {
            this.name = name;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(element instanceof Variable && name.equals(((Variable) element).getName())) {
                variables.add((Variable) element);
            }
            super.visitElement(element);
        }

        public static Set<Variable> visit(@NotNull PsiElement scope, @NotNull String name) {
            MyVariableRecursiveElementVisitor visitor = new MyVariableRecursiveElementVisitor(name);
            scope.acceptChildren(visitor);
            return visitor.variables;
        }
    }


    /**
     * Find string values based on a given PsiElement and its references
     *
     * - render(true === true ? 'foo.twig.html' : 'foobar.twig.html')
     * - foo(self::foo), foo($var), foo($this->foo), ...
     * - render($foo ?? 'foo.twig.html')
     */
    public static class StringResolver {
        public static Collection<String> findStringValues(@NotNull PsiElement psiElement) {
            Collection<String> strings = new HashSet<>();

            if (psiElement instanceof StringLiteralExpression) {
                strings.add(resolveString((StringLiteralExpression) psiElement));
            } else if(psiElement instanceof TernaryExpression) {
                // render(true === true ? 'foo.twig.html' : 'foobar.twig.html')
                for (PhpPsiElement phpPsiElement : new PhpPsiElement[]{((TernaryExpression) psiElement).getTrueVariant(), ((TernaryExpression) psiElement).getFalseVariant()}) {
                    if (phpPsiElement == null) {
                        continue;
                    }

                    if (phpPsiElement instanceof StringLiteralExpression) {
                        strings.add(resolveString((StringLiteralExpression) phpPsiElement));
                    } else if(phpPsiElement instanceof PhpReference) {
                        strings.add(resolvePhpReference((PhpReference) phpPsiElement));
                    }
                }
            } else if(psiElement instanceof PhpReference) {
                // foo(self::foo)
                // foo($this->foo)
                // foo($var)
                strings.add(resolvePhpReference((PhpReference) psiElement));
            } else if(psiElement instanceof BinaryExpression) {
                // render($foo ?? 'foo.twig.html')
                PsiElement phpPsiElement = ((BinaryExpression) psiElement).getRightOperand();

                if (phpPsiElement instanceof StringLiteralExpression) {
                    strings.add(resolveString((StringLiteralExpression) phpPsiElement));
                } else if(phpPsiElement instanceof PhpReference) {
                    strings.add(resolvePhpReference((PhpReference) phpPsiElement));
                }
            }

            return strings;
        }

        @Nullable
        private static String resolveString(@NotNull StringLiteralExpression parameter) {
            String contents = parameter.getContents();
            return StringUtils.isBlank(contents) ? null : contents;
        }

        @Nullable
        private static String resolvePhpReference(@NotNull PhpReference parameter) {
            for (PhpNamedElement phpNamedElement : parameter.resolveLocal()) {
                // foo(self::foo)
                // foo($this->foo)
                if (phpNamedElement instanceof Field) {
                    PsiElement defaultValue = ((Field) phpNamedElement).getDefaultValue();
                    if (defaultValue instanceof StringLiteralExpression) {
                        return resolveString((StringLiteralExpression) defaultValue);
                    }
                }

                // foo($var) => $var = 'test.html.twig'
                if (phpNamedElement instanceof Variable) {
                    PsiElement assignmentExpression = phpNamedElement.getParent();
                    if (assignmentExpression instanceof AssignmentExpression) {
                        PhpPsiElement value = ((AssignmentExpression) assignmentExpression).getValue();
                        if (value instanceof StringLiteralExpression) {
                            return resolveString((StringLiteralExpression) value);
                        }
                    }
                }
            }

            return null;
        }
    }

    /**
     * Adds a parameter with the given fully qualified class name and variable name to a method.
     * This method handles use statement insertion and parameter list modification with reformatting.
     * The parameter is inserted before any optional parameters to maintain valid PHP syntax.
     *
     * @param method The method to add the parameter to
     * @param fqn The fully qualified class name for the parameter type (e.g., "\\Symfony\\Component\\Console\\Style\\SymfonyStyle")
     * @param variableName The variable name for the parameter (e.g., "io")
     */
    public static void addParameterToMethod(@NotNull Method method, @NotNull String fqn, @NotNull String variableName) {
        PhpClass phpClass = method.getContainingClass();
        if (phpClass == null) {
            return;
        }

        Project project = method.getProject();

        insertUseIfNecessary(phpClass, fqn);

        PsiFile file = method.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        String shortName = fqn.substring(fqn.lastIndexOf('\\') + 1);
        String newParameter = shortName + " $" + variableName;

        Parameter[] existingParams = method.getParameters();
        int insertOffset;
        String textToInsert;

        if (existingParams.length > 0) {
            // Find the first optional parameter (has default value)
            Parameter firstOptionalParam = null;
            Parameter lastRequiredParam = null;

            for (Parameter param : existingParams) {
                if (param.getDefaultValue() != null) {
                    if (firstOptionalParam == null) {
                        firstOptionalParam = param;
                    }
                } else {
                    lastRequiredParam = param;
                }
            }

            if (firstOptionalParam != null && lastRequiredParam == null) {
                // All existing parameters are optional, insert at the beginning
                insertOffset = existingParams[0].getTextRange().getStartOffset();
                textToInsert = newParameter + ", ";
            } else if (firstOptionalParam != null) {
                // Insert after the last required parameter
                insertOffset = lastRequiredParam.getTextRange().getEndOffset();
                textToInsert = ", " + newParameter;
            } else {
                // No optional parameters, append at the end
                Parameter lastParam = existingParams[existingParams.length - 1];
                insertOffset = lastParam.getTextRange().getEndOffset();
                textToInsert = ", " + newParameter;
            }
        } else {
            String methodText = method.getText();
            int methodStartOffset = method.getTextRange().getStartOffset();

            int openParenIndex = methodText.indexOf('(');
            if (openParenIndex == -1) {
                return;
            }

            insertOffset = methodStartOffset + openParenIndex + 1;
            textToInsert = newParameter;
        }

        document.insertString(insertOffset, textToInsert);

        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        PsiFile freshFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (freshFile != null) {
            PhpClass freshPhpClass = PsiTreeUtil.findChildOfType(freshFile, PhpClass.class);
            if (freshPhpClass != null) {
                Method freshMethod = freshPhpClass.findOwnMethodByName(method.getName());
                if (freshMethod != null) {
                    reformatMethodSignature(project, freshMethod);
                }
            }
        }
    }

    /**
     * Reformats the method signature (from method start to body start).
     *
     * @param project The project
     * @param method The method to reformat
     */
    public static void reformatMethodSignature(@NotNull Project project, @NotNull Method method) {
        PsiFile containingFile = method.getContainingFile();

        GroupStatement body = PsiTreeUtil.findChildOfType(method, GroupStatement.class);
        if (body == null) {
            return;
        }

        int startOffset = method.getTextRange().getStartOffset();
        int endOffset = body.getTextRange().getStartOffset();

        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        try {
            codeStyleManager.reformatRange(containingFile, startOffset, endOffset);
        } catch (com.intellij.util.IncorrectOperationException e) {
            // Ignore formatting errors
        }
    }
}
