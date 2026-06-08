package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpCallInstruction;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.extension.PluginConfigurationExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.PluginConfigurationExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import kotlin.Triple;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpMethodVariableResolveUtil {
    private static final int MAX_CONTEXT_RESOLVE_DEPTH = 2;
    private static final Set<String> ARRAY_CONTEXT_FUNCTIONS = new HashSet<>(Arrays.asList(
        "array_merge", "array_merge_recursive", "array_push", "array_replace"
    ));

    /**
     * search for twig template variable on common use cases
     * <p>
     * $this->render('foobar.html.twig', $foobar)
     * $this->render('foobar.html.twig', ['foobar' => $var]))
     * $this->render('foobar.html.twig', array_merge($foobar, ['foobar' => $var]))
     * $this->render('foobar.html.twig', array_merge_recursive($foobar, ['foobar' => $var]))
     * $this->render('foobar.html.twig', array_push($foobar, ['foobar' => $var]))
     * $this->render('foobar.html.twig', array_replace($foobar, ['foobar' => $var]))
     * $this->render('foobar.html.twig', $foobar + ['foobar' => $var])
     * $this->render('foobar.html.twig', $foobar += ['foobar' => $var])
     */
    public static Map<String, PsiVariable> collectMethodVariables(@NotNull Function method, @NotNull Collection<String> templateNames) {
        if (templateNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, PsiVariable> collectedTypes = new HashMap<>();
        ResolveContext context = new ResolveContext();

        Set<String> normalizedTemplateNames = templateNames.stream()
            .map(TwigUtil::normalizeTemplateName)
            .collect(Collectors.toSet());

        for(PsiElement var: collectPossibleTemplateArrays(method, normalizedTemplateNames)) {
            collectVariablesForPsiElement(method, collectedTypes, var, 0, context);
        }

        return collectedTypes;
    }

    private static void collectVariablesForPsiElement(@NotNull Function method, @NotNull Map<String, PsiVariable> collectedTypes, @NotNull PsiElement psiElement, int depth, @NotNull ResolveContext context) {
        if (depth > MAX_CONTEXT_RESOLVE_DEPTH) {
            return;
        }

        if (psiElement instanceof ArrayCreationExpression arrayCreationExpression) {
            // ['foobar' => $var]
            collectedTypes.putAll(getTypesOnArrayHash(method, arrayCreationExpression, depth, context));
        } else if (psiElement instanceof Variable variable) {
            // $this->render('foo.html.twig', $templateData)
            PsiElement resolvedVariable = variable.resolve();
            if (resolvedVariable instanceof Variable resolvedTemplateData) {
                collectedTypes.putAll(collectOnVariableReferences(method, resolvedTemplateData, depth, context));
            }
        } else if (psiElement instanceof FunctionReference functionReference) {
            if (isArrayContextFunction(functionReference)) {
                // array_merge($templateData, ['foobar' => $var])
                // array_replace($templateData, $this->createTemplateData())
                for (PsiElement parameter : functionReference.getParameters()) {
                    collectVariablesForPsiElement(method, collectedTypes, parameter, depth, context);
                }
            } else {
                // $this->createTemplateData()
                collectVariablesForLocalFunctionReference(method, collectedTypes, functionReference, depth, context);
            }
        } else if (psiElement instanceof BinaryExpression binaryExpression && psiElement.getNode().getElementType() == PhpElementTypes.ADDITIVE_EXPRESSION) {
            // $templateData + ['foobar' => $var]
            PsiElement leftOperand = binaryExpression.getLeftOperand();
            if (leftOperand != null) {
                collectVariablesForPsiElement(method, collectedTypes, leftOperand, depth, context);
            }

            PsiElement rightOperand = binaryExpression.getRightOperand();
            if (rightOperand != null) {
                collectVariablesForPsiElement(method, collectedTypes, rightOperand, depth, context);
            }
        } else if (psiElement instanceof SelfAssignmentExpression selfAssignmentExpression) {
            // $templateData += ['foobar' => $var]
            PhpPsiElement variable = selfAssignmentExpression.getVariable();
            if (variable != null) {
                collectVariablesForPsiElement(method, collectedTypes, variable, depth, context);
            }

            PhpPsiElement value = selfAssignmentExpression.getValue();
            if (value != null) {
                collectVariablesForPsiElement(method, collectedTypes, value, depth, context);
            }
        } else if (isArrayUnpackValue(psiElement)) {
            // ...$templateData
            // ...$this->createTemplateData()
            PhpPsiElement arrayValue = ((PhpPsiElement) psiElement).getFirstPsiChild();
            if (arrayValue != null) {
                collectVariablesForPsiElement(method, collectedTypes, arrayValue, depth, context);
            }
        }
    }

    private static boolean isArrayContextFunction(@NotNull FunctionReference functionReference) {
        String name = functionReference.getName();
        return name != null && ARRAY_CONTEXT_FUNCTIONS.contains(name.toLowerCase(Locale.ROOT));
    }

    private static void collectVariablesForLocalFunctionReference(
        @NotNull Function method,
        @NotNull Map<String, PsiVariable> collectedTypes,
        @NotNull FunctionReference functionReference,
        int depth,
        @NotNull ResolveContext context
    ) {
        if (depth >= MAX_CONTEXT_RESOLVE_DEPTH) {
            return;
        }

        for (PhpNamedElement phpNamedElement : functionReference.resolveLocal()) {
            if (!(phpNamedElement instanceof Function targetFunction)) {
                continue;
            }

            if (targetFunction == method || !isLocalContextFunction(method, functionReference, targetFunction)) {
                continue;
            }

            collectVariablesForFunctionReturns(targetFunction, collectedTypes, depth + 1, context);
        }
    }

    private static boolean isLocalContextFunction(@NotNull Function method, @NotNull FunctionReference functionReference, @NotNull Function targetFunction) {
        if (functionReference instanceof MethodReference) {
            if (!(method instanceof Method sourceMethod) || !(targetFunction instanceof Method targetMethod)) {
                return false;
            }

            PhpClass sourceClass = sourceMethod.getContainingClass();
            PhpClass targetClass = targetMethod.getContainingClass();

            return sourceClass != null && sourceClass.equals(targetClass);
        }

        return method.getContainingFile() != null && method.getContainingFile().equals(targetFunction.getContainingFile());
    }

    /**
     * Resolves local context helper returns.
     * <pre>$this->render('foo.html.twig', $this->createTemplateData());</pre>
     */
    private static void collectVariablesForFunctionReturns(
        @NotNull Function function,
        @NotNull Map<String, PsiVariable> collectedTypes,
        int depth,
        @NotNull ResolveContext context
    ) {
        if (depth > MAX_CONTEXT_RESOLVE_DEPTH || !context.visitFunction(function)) {
            return;
        }

        for (PhpReturn phpReturn : PsiTreeUtil.findChildrenOfType(function, PhpReturn.class)) {
            Function returnScope = PsiTreeUtil.getParentOfType(phpReturn, Function.class);
            if (!function.equals(returnScope)) {
                continue;
            }

            PhpPsiElement returnPsiElement = phpReturn.getFirstPsiChild();
            if (returnPsiElement != null) {
                collectVariablesForPsiElement(function, collectedTypes, returnPsiElement, depth, context);
            }
        }
    }

    /**
     * Search for PSI elements that can hold Twig template variables in the current method scope.
     * <p>
     * Sources:
     * <pre>
     *   - matching annotation/attribute return values: return ['foo' => $bar] / return $templateVars
     *   - render-like calls discovered by {@link #visitRenderTemplateFunctions(Function, Consumer)}
     * </pre>
     * <p>
     * For render-like calls, the variables argument index depends on the method signature and is
     * resolved via {@link #getTemplateParameterIndex(FunctionReference)}:
     * <pre>
     *   - render/renderView/stream: index 1
     *   - renderBlock/renderBlockView: index 2
     * </pre>
     */
    @NotNull
    private static List<PsiElement> collectPossibleTemplateArrays(@NotNull Function method, @NotNull Set<String> normalizedTemplateNames) {
        if (normalizedTemplateNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<PsiElement> collectedTemplateVariables = new ArrayList<>();

        // twig render calls:
        // $twig->render('foo', $vars);
        Set<FunctionReference> references = new HashSet<>();
        boolean[] collectControllerReturn = new boolean[] { false };
        visitRenderTemplateFunctions(method, triple -> {
            if (!normalizedTemplateNames.contains(triple.getFirst())) {
                return;
            }

            FunctionReference functionScope = triple.getThird();
            if (functionScope != null) {
                references.add(functionScope);
            } else {
                collectControllerReturn[0] = true;
            }
        });

        if (collectControllerReturn[0]) {
            // Annotation / attribute controller
            for(PhpReturn phpReturn : PsiTreeUtil.findChildrenOfType(method, PhpReturn.class)) {
                PhpPsiElement returnPsiElement = phpReturn.getFirstPsiChild();

                // @TODO: think of support all types here
                // return $template
                // return array('foo' => $var)
                if(returnPsiElement instanceof Variable || returnPsiElement instanceof ArrayCreationExpression || returnPsiElement instanceof FunctionReference) {
                    collectedTemplateVariables.add(returnPsiElement);
                }
            }
        }

        for(FunctionReference methodReference : references) {
            PsiElement templateParameter = TemplateRenderVisitor.findTemplateContextParameter(methodReference);
            if(templateParameter != null) {
                collectedTemplateVariables.add(templateParameter);
            }
        }

        return collectedTemplateVariables;
    }

    /**
     * Resolve the argument index that contains template variables for a render-like call.
     *
     * <pre>
     *   render(string $view, array $parameters = [])
     *   renderView(string $view, array $parameters = [])
     *   stream(string $view, array $parameters = [])
     *   renderBlock(string $view, string $block, array $parameters = [])
     *   renderBlockView(string $view, string $block, array $parameters = [])
     * </pre>
     */
    private static int getTemplateParameterIndex(@NotNull FunctionReference methodReference) {
        String methodName = methodReference.getName();
        if ("renderBlock".equalsIgnoreCase(methodName) || "renderBlockView".equalsIgnoreCase(methodName)) {
            return 2;
        }

        return 1;
    }


    /**
     * search for references of variable declaration and collect the types
     *
     * @param function should be function / method scope
     * @param variable the variable declaration psi $var = array();
     */
    @NotNull
    private static Map<String, PsiVariable> collectOnVariableReferences(@NotNull Function function, @NotNull Variable variable, int depth, @NotNull ResolveContext context) {
        Map<String, PsiVariable> collectedTypes = new HashMap<>();
        if (depth > MAX_CONTEXT_RESOLVE_DEPTH || !context.visitVariable(variable)) {
            return collectedTypes;
        }

        for (Variable scopeVar : PhpElementsUtil.getVariablesInScopeByName(function, variable.getName())) {
            PsiElement parent = scopeVar.getParent();
            if (parent instanceof ArrayAccessExpression) {
                // $template['variable'] = $foo
                Pair<String, PsiVariable> pair = getTypesOnArrayIndex((ArrayAccessExpression) parent);
                if (pair != null) {
                    collectedTypes.put(pair.getFirst(), pair.getSecond());
                }
            } else if (parent instanceof SelfAssignmentExpression) {
                PhpPsiElement value = ((SelfAssignmentExpression) parent).getValue();
                PhpPsiElement assignmentVariable = ((SelfAssignmentExpression) parent).getVariable();
                if (value != null && assignmentVariable == scopeVar) {
                    collectVariablesForPsiElement(function, collectedTypes, value, depth, context);
                }
            } else if (parent instanceof AssignmentExpression) {
                // array('foo' => $var)
                PhpPsiElement value = ((AssignmentExpression) parent).getValue();
                PhpPsiElement assignmentVariable = ((AssignmentExpression) parent).getVariable();
                if (value != null && assignmentVariable == scopeVar) {
                    collectVariablesForPsiElement(function, collectedTypes, value, depth, context);
                }
            }
        }

        return collectedTypes;
    }

    /**
     * $template['var'] = $foo
     */
    @Nullable
    private static Pair<String, PsiVariable> getTypesOnArrayIndex(@NotNull ArrayAccessExpression arrayAccessExpression) {
        ArrayIndex arrayIndex = arrayAccessExpression.getIndex();
        if(arrayIndex != null && arrayIndex.getValue() instanceof StringLiteralExpression) {

            String variableName = ((StringLiteralExpression) arrayIndex.getValue()).getContents();
            Set<String> variableTypes = new HashSet<>();

            PsiElement parent = arrayAccessExpression.getParent();
            if(parent instanceof AssignmentExpression) {
                PsiElement arrayValue = ((AssignmentExpression) parent).getValue();
                if(arrayValue instanceof PhpTypedElement) {
                    variableTypes.addAll(((PhpTypedElement) arrayValue).getType().getTypes());
                }

                return Pair.create(variableName, new PsiVariable(
                    variableTypes,
                    arrayValue == null ? Collections.emptySet() : FormFieldResolver.getFormTypeFqnsFromFormFactory(arrayValue)
                ));
            } else {
                return Pair.create(variableName, new PsiVariable(variableTypes));
            }
        }

        return null;
    }

    /**
     *  array('foo' => $var, 'bar' => $bar)
     */
    public static Map<String, PsiVariable> getTypesOnArrayHash(@NotNull ArrayCreationExpression arrayCreationExpression) {
        return getTypesOnArrayHash(null, arrayCreationExpression, 0, new ResolveContext());
    }

    private static Map<String, PsiVariable> getTypesOnArrayHash(@Nullable Function method, @NotNull ArrayCreationExpression arrayCreationExpression, int depth, @NotNull ResolveContext context) {
        Map<String, PsiVariable> collectedTypes = new HashMap<>();

        for (PsiElement child : arrayCreationExpression.getChildren()) {
            if (child instanceof ArrayHashElement arrayHashElement) {
                collectArrayHashElementTypes(collectedTypes, arrayHashElement);
            } else if (method != null && isArrayUnpackValue(child)) {
                PhpPsiElement arrayValue = ((PhpPsiElement) child).getFirstPsiChild();
                if (arrayValue != null) {
                    collectVariablesForPsiElement(method, collectedTypes, arrayValue, depth, context);
                }
            }
        }

        return collectedTypes;
    }

    /**
     * Collects direct string-key array entries.
     * <pre>['foobar' => $var]</pre>
     */
    private static void collectArrayHashElementTypes(@NotNull Map<String, PsiVariable> collectedTypes, @NotNull ArrayHashElement arrayHashElement) {
        if (arrayHashElement.getKey() instanceof StringLiteralExpression stringLiteralExpression) {
            String variableName = stringLiteralExpression.getContents();
            Set<String> variableTypes = new HashSet<>();
            PsiElement arrayValue = arrayHashElement.getValue();

            if (arrayValue instanceof PhpTypedElement phpTypedElement) {
                variableTypes.addAll(phpTypedElement.getType().getTypes());
            }

            collectedTypes.put(variableName, new PsiVariable(
                variableTypes,
                arrayValue == null ? Collections.emptySet() : FormFieldResolver.getFormTypeFqnsFromFormFactory(arrayValue)
            ));
        }
    }

    /**
     * Detects PHP array unpack values.
     * <pre>['headline' => 'Foo', ...$templateData, ...$this->createTemplateData()]</pre>
     */
    private static boolean isArrayUnpackValue(@NotNull PsiElement psiElement) {
        return psiElement instanceof PhpPsiElement &&
            psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE &&
            psiElement.getNode().findChildByType(PhpTokenTypes.opVARIADIC) != null;
    }

    private static class ResolveContext {
        private final Set<Variable> variables = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<Function> functions = Collections.newSetFromMap(new IdentityHashMap<>());

        private boolean visitVariable(@NotNull Variable variable) {
            return variables.add(variable);
        }

        private boolean visitFunction(@NotNull Function function) {
            return functions.add(function);
        }
    }

    /**
     * Visit method scope for template renders, also via annotation of the method itself
     * <p>
     * As annotations are not in scope of the method itself
     */
    public static void visitRenderTemplateFunctions(@NotNull Function function, @NotNull Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
        PhpDocComment docComment = function.getDocComment();
        for (PhpDocTag phpDocTag : PsiTreeUtil.getChildrenOfTypeAsList(docComment, PhpDocTag.class)) {
            TemplateRenderVisitor.processDocTag(phpDocTag, consumer);
        }

        for (PhpAttributesList phpAttributesList : PsiTreeUtil.getChildrenOfTypeAsList(function, PhpAttributesList.class)) {
            if (phpAttributesList.getParent() instanceof Method phpAttributeMethod) {
                TemplateRenderVisitor.processMethodAttributes(phpAttributeMethod, consumer);
            }
        }

        NotNullLazyValue<Set<String>> lazyMethodNamesCollector = TemplateRenderVisitor.createLazyMethodNamesCollector(function.getProject());

        PhpControlFlowUtil.processFlow(function.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processPhpCallInstruction(PhpCallInstruction instruction) {
                if (instruction.getFunctionReference() instanceof MethodReference methodReference) {
                    TemplateRenderVisitor.processMethodReference(methodReference, lazyMethodNamesCollector, consumer);
                }
                return super.processPhpCallInstruction(instruction);
            }
        });
    }

    public static class TemplateRenderVisitor {
        public static void processMethodAttributes(@NotNull Method method, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            for (String templateAnnotationClass : TwigUtil.TEMPLATE_ANNOTATION_CLASS) {
                Collection<@NotNull PhpAttribute> attributes = method.getAttributes(templateAnnotationClass);
                for (PhpAttribute attribute : attributes) {
                    if (attribute.getArguments().isEmpty()) {
                        // #[@Template()]
                        visitMethodForGuessing(method, consumer);
                    } else {
                        // [@Template("foobar.html.twig")]
                        // #[@Template(template: "foobar.html.twig")]
                        String template = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "template");
                        if (StringUtils.isNotBlank(template)) {
                            addTemplateWithScope(template, method, null, consumer);
                        }
                    }
                }
            }
        }

        @NotNull
        private static Set<String> collectMethodInner(@NotNull Project project) {
            Set<String> methods = new HashSet<>();

            PluginConfigurationExtension[] extensions = Symfony2ProjectComponent.PLUGIN_CONFIGURATION_EXTENSION.getExtensions();
            if(extensions.length > 0) {
                PluginConfigurationExtensionParameter pluginConfiguration = new PluginConfigurationExtensionParameter(project);
                for (PluginConfigurationExtension extension : extensions) {
                    extension.invokePluginConfiguration(pluginConfiguration);
                }

                methods.addAll(pluginConfiguration.getTemplateUsageMethod());
            }
            return methods;
        }

        public static @NotNull NotNullLazyValue<Set<String>> createLazyMethodNamesCollector(@NotNull Project project) {
            return NotNullLazyValue.lazy(() -> collectMethodInner(project));
        }

        public static void processMethodReference(@NotNull MethodReference methodReference, NotNullLazyValue<Set<String>> methods, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            String methodName = methodReference.getName();
            if (methodName == null) {
                return;
            }

            String normalizedMethodName = methodName.toLowerCase(Locale.ROOT);
            boolean configuredMethod = methods.get().contains(methodName);
            if (!configuredMethod && Stream.of("render", "htmltemplate", "texttemplate", "renderblock", "renderblockview").noneMatch(normalizedMethodName::contains)) {
                return;
            }

            Collection<String> namedArguments = getTemplateNamedArguments(methodReference);
            PsiElement templateParameter = findTemplateParameter(methodReference, namedArguments, configuredMethod && namedArguments.isEmpty());
            if (templateParameter == null) {
                return;
            }

            addTemplateParameterScopes(methodReference, templateParameter, consumer);
        }

        @Nullable
        private static PsiElement findTemplateParameter(@NotNull MethodReference methodReference, @NotNull Collection<String> namedArguments, boolean allowNamedFirstParameterFallback) {
            ParameterList parameterList = methodReference.getParameterList();
            if (parameterList == null) {
                return null;
            }

            for (String namedArgument : namedArguments) {
                PsiElement namedParameter = findNamedArgument(parameterList, namedArgument);
                if (namedParameter != null) {
                    return namedParameter;
                }
            }

            PsiElement firstParameter = PsiElementUtils.getMethodParameterPsiElementAt(parameterList, 0);
            if (firstParameter == null || (!allowNamedFirstParameterFallback && isNamedArgument(firstParameter))) {
                return null;
            }

            return firstParameter;
        }

        @Nullable
        private static PsiElement findTemplateContextParameter(@NotNull FunctionReference methodReference) {
            ParameterList parameterList = methodReference.getParameterList();
            if (parameterList == null) {
                return null;
            }

            for (String namedArgument : Arrays.asList("parameters", "context")) {
                PsiElement namedParameter = findNamedArgument(parameterList, namedArgument);
                if (namedParameter != null) {
                    return namedParameter;
                }
            }

            PsiElement templateParameter = PsiElementUtils.getMethodParameterPsiElementAt(
                parameterList,
                getTemplateParameterIndex(methodReference)
            );

            if (templateParameter == null || isNamedArgument(templateParameter)) {
                return null;
            }

            return templateParameter;
        }

        @NotNull
        private static Collection<String> getTemplateNamedArguments(@NotNull MethodReference methodReference) {
            String methodName = methodReference.getName();
            if (methodName == null) {
                return Collections.emptyList();
            }

            String normalizedMethodName = methodName.toLowerCase(Locale.ROOT);
            if (normalizedMethodName.contains("htmltemplate") || normalizedMethodName.contains("texttemplate")) {
                return Collections.singletonList("template");
            }

            if (normalizedMethodName.contains("renderblock") || normalizedMethodName.contains("renderblockview")) {
                return Collections.singletonList("view");
            }

            if (normalizedMethodName.contains("render") || "stream".equalsIgnoreCase(methodName)) {
                return Arrays.asList("view", "name", "template");
            }

            return Collections.emptyList();
        }

        @Nullable
        private static PsiElement findNamedArgument(@NotNull ParameterList parameterList, @NotNull String argumentName) {
            for (PsiElement parameter : parameterList.getParameters()) {
                if (argumentName.equalsIgnoreCase(getNamedArgumentName(parameter))) {
                    return parameter;
                }
            }

            return null;
        }

        private static boolean isNamedArgument(@NotNull PsiElement parameter) {
            return getNamedArgumentName(parameter) != null;
        }

        @Nullable
        private static String getNamedArgumentName(@NotNull PsiElement parameter) {
            PsiElement colon = PsiTreeUtil.prevCodeLeaf(parameter);
            if (colon == null || colon.getNode().getElementType() != PhpTokenTypes.opCOLON) {
                return null;
            }

            PsiElement argumentName = PsiTreeUtil.prevCodeLeaf(colon);
            if (argumentName == null || argumentName.getNode().getElementType() != PhpTokenTypes.IDENTIFIER) {
                return null;
            }

            return argumentName.getText();
        }

        private static void addTemplateParameterScopes(@NotNull MethodReference methodReference, @NotNull PsiElement parameter, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            if (parameter instanceof StringLiteralExpression) {
                addStringLiteralScope(methodReference, (StringLiteralExpression) parameter, consumer);
            } else if(parameter instanceof TernaryExpression) {
                // render(true === true ? 'foo.twig.html' : 'foobar.twig.html')
                for (PhpPsiElement phpPsiElement : new PhpPsiElement[]{((TernaryExpression) parameter).getTrueVariant(), ((TernaryExpression) parameter).getFalseVariant()}) {
                    switch (phpPsiElement) {
                        case StringLiteralExpression stringLiteralExpression -> addStringLiteralScope(methodReference, stringLiteralExpression, consumer);
                        case PhpReference phpReference -> resolvePhpReference(methodReference, phpReference, consumer);
                        case null, default -> {
                        }
                    }
                }
            } else if(parameter instanceof AssignmentExpression) {
                // $this->render($template = 'foo.html.twig')
                PhpPsiElement value = ((AssignmentExpression) parameter).getValue();
                if(value instanceof StringLiteralExpression) {
                    addStringLiteralScope(methodReference, (StringLiteralExpression) value, consumer);
                }
            } else if(parameter instanceof PhpReference) {
                resolvePhpReference(methodReference, parameter, consumer);
            } else if(parameter instanceof BinaryExpression) {
                // render($foo ?? 'foo.twig.html')
                PsiElement phpPsiElement = ((BinaryExpression) parameter).getRightOperand();

                if (phpPsiElement instanceof StringLiteralExpression) {
                    addStringLiteralScope(methodReference, (StringLiteralExpression) phpPsiElement, consumer);
                } else if(phpPsiElement instanceof PhpReference) {
                    resolvePhpReference(methodReference, phpPsiElement, consumer);
                }
            }
        }

        private static void resolvePhpReference(@NotNull MethodReference methodReference, PsiElement parameter, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            for (PhpNamedElement phpNamedElement : ((PhpReference) parameter).resolveLocal()) {
                // foo(self::foo)
                // foo($this->foo)
                if (phpNamedElement instanceof Field) {
                    PsiElement defaultValue = ((Field) phpNamedElement).getDefaultValue();
                    if (defaultValue instanceof StringLiteralExpression) {
                        addStringLiteralScope(methodReference, (StringLiteralExpression) defaultValue, consumer);
                    }
                } else if (phpNamedElement instanceof Variable) {
                    // foo($var) => $var = 'test.html.twig'
                    PsiElement assignmentExpression = phpNamedElement.getParent();
                    if (assignmentExpression instanceof AssignmentExpression) {
                        PhpPsiElement value = ((AssignmentExpression) assignmentExpression).getValue();
                        if (value instanceof StringLiteralExpression) {
                            addStringLiteralScope(methodReference, (StringLiteralExpression) value, consumer);
                        }
                    }
                } else if (phpNamedElement instanceof Parameter) {
                    // function foobar($defaultParameter = 'default-function-parameter.html.twig')
                    PsiElement value = ((Parameter) phpNamedElement).getDefaultValue();
                    if (value instanceof StringLiteralExpression) {
                        addStringLiteralScope(methodReference, (StringLiteralExpression) value, consumer);
                    }
                }
            }
        }

        private static void addStringLiteralScope(@NotNull MethodReference methodReference, @NotNull StringLiteralExpression defaultValue, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            String contents = defaultValue.getContents();
            if (StringUtils.isBlank(contents) || !contents.endsWith(".twig")) {
                return;
            }

            Function parentOfType = PsiTreeUtil.getParentOfType(methodReference, Function.class);
            if (parentOfType == null) {
                return;
            }

            addTemplateWithScope(contents, parentOfType, methodReference, consumer);
        }

        /**
         * "@Template("foobar.html.twig")"
         * "@Template(template="foobar.html.twig")"
         */
        public static void processDocTag(@NotNull PhpDocTag phpDocTag, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            processDocTag(phpDocTag, AnnotationBackportUtil.getUseImportMap(phpDocTag), consumer);
        }

        public static void processDocTag(@NotNull PhpDocTag phpDocTag, @NotNull Map<String, String> fileImports, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            // "@var" and user non-related tags don't need an action
            if(AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                return;
            }

            if(fileImports.isEmpty()) {
                return;
            }

            String annotationFqnName = AnnotationBackportUtil.getClassNameReference(phpDocTag, fileImports);
            if (Arrays.stream(TwigUtil.TEMPLATE_ANNOTATION_CLASS).noneMatch(s -> s.equals(annotationFqnName))) {
                return;
            }

            String template = AnnotationUtil.getPropertyValueOrDefault(phpDocTag, "template");
            if (template == null) {
                // see \Sensio\Bundle\FrameworkExtraBundle\Templating\TemplateGuesser
                // App\Controller\MyNiceController::myAction => my_nice/my.html.twig
                Method methodScope = AnnotationBackportUtil.getMethodScope(phpDocTag);
                if(methodScope != null) {
                    visitMethodForGuessing(methodScope, consumer);
                }
            } else if(template.endsWith(".twig")) {
                Method methodScope = AnnotationBackportUtil.getMethodScope(phpDocTag);
                if(methodScope != null) {
                    addTemplateWithScope(template, methodScope, null, consumer);
                }
            }
        }

        private static void visitMethodForGuessing(@NotNull Method methodScope, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            PhpClass phpClass = methodScope.getContainingClass();
            if (phpClass != null) {
                // App\Controller\  "MyNice"  Controller
                Matcher matcher = Pattern.compile("Controller\\\\(.+)Controller$", Pattern.MULTILINE).matcher(StringUtils.stripStart(phpClass.getFQN(), "\\"));
                if(matcher.find()){
                    String group = underscore(matcher.group(1).replace("\\", "/"));
                    String name = methodScope.getName();

                    // __invoke is using controller as template name
                    if (name.equals("__invoke")) {
                        addTemplateWithScope(group + ".html.twig", methodScope, null, consumer);
                    } else {
                        String action = name.endsWith("Action") ? name.substring(0, name.length() - "Action".length()) : name;
                        addTemplateWithScope(group + "/" + underscore(action) + ".html.twig", methodScope, null, consumer);
                    }
                }
            }
        }

        private static void addTemplateWithScope(@NotNull String contents, @NotNull PhpNamedElement scope, @Nullable FunctionReference functionReference, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            String s = TwigUtil.normalizeTemplateName(contents);
            consumer.accept(new Triple<>(s, scope, functionReference));
        }
    }
}
