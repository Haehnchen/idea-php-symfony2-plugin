package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.StaticVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormVarsResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.TwigTypeResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.captureVariableOrField;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTypeResolveUtil {

    /**
     * {# variable \AppBundle\Entity\Foo[] #}
     */
    public static final String DEPRECATED_DOC_TYPE_PATTERN = "\\{#[\\s]+(?<var>[\\w]+)[\\s]+(?<class>[\\w\\\\\\[\\]]+)[\\s]+#}";

    /**
     * {# @var variable \AppBundle\Entity\Foo[] #}
     * {# @var variable \AppBundle\Entity\Foo #}
     */
    private static final String DOC_TYPE_PATTERN_CLASS_SECOND = "@var\\s+(?<var>\\w+)\\s+(?<class>[\\w\\\\\\[\\]]+)\\s*";

    /**
     * {# @var \AppBundle\Entity\Foo[] variable #}
     * {# @var \AppBundle\Entity\Foo variable #}
     */
    private static final String DOC_TYPE_PATTERN_CLASS_FIRST = "@var\\s+(?<class>[\\w\\\\\\[\\]]+)\\s+(?<var>\\w+)\\s*";

    public static final Pattern[] INLINE_DOC_REGEX = {
        Pattern.compile(DOC_TYPE_PATTERN_CLASS_SECOND, Pattern.MULTILINE),
        Pattern.compile(DOC_TYPE_PATTERN_CLASS_FIRST, Pattern.MULTILINE),
        Pattern.compile(DEPRECATED_DOC_TYPE_PATTERN),
    };

    // for supporting completion and navigation of one line element
    public static final String[] DOC_TYPE_PATTERN_SINGLE  = new String[] {
        "^(?<var>[\\w]+)[\\s]+(?<class>[\\w\\\\\\[\\]]+)[\\s]*$",
        DOC_TYPE_PATTERN_CLASS_SECOND,
        DOC_TYPE_PATTERN_CLASS_FIRST
    };

    private static final String[] PROPERTY_SHORTCUTS = new String[] {"get", "is", "has"};

    private static final ExtensionPointName<TwigFileVariableCollector> TWIG_FILE_VARIABLE_COLLECTORS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.TwigVariableCollector"
    );

    private static final Key<CachedValue<Map<String, String>>> FILE_VARIABLE_DOC_BLOCK_CACHE = new Key<>("TWIG_FILE_VARIABLE_DOC_BLOCK");

    private static final TwigTypeResolver[] TWIG_TYPE_RESOLVERS = new TwigTypeResolver[] {
        new FormVarsResolver(),
        new FormFieldResolver(),
    };

    @NotNull
    public static Collection<String> formatPsiTypeNameWithCurrent(@NotNull PsiElement psiElement) {
        Collection<String> strings = new ArrayList<>(formatPsiTypeName(psiElement));
        strings.add(psiElement.getText());
        return strings;
    }

    /**
     * Get items before foo.bar.car, foo.bar.car()
     *
     * ["foo", "bar"]
     */
    @NotNull
    public static Collection<String> formatPsiTypeName(@NotNull PsiElement psiElement) {
        String typeNames = PhpElementsUtil.getPrevSiblingAsTextUntil(psiElement, PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
            PlatformPatterns.psiElement(PsiWhiteSpace.class
        )));

        if(typeNames.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if(typeNames.endsWith(".")) {
            typeNames = typeNames.substring(0, typeNames.length() -1);
        }

        Collection<String> possibleTypes = new ArrayList<>();
        if(typeNames.contains(".")) {
            possibleTypes.addAll(Arrays.asList(typeNames.split("\\.")));
        } else {
            possibleTypes.add(typeNames);
        }

        return possibleTypes;
    }

    /**
     * Collects all possible variables in given path for last given item of "typeName"
     *
     * @param types Variable path "foo.bar" => ["foo", "bar"]
     * @return types for last item of typeName parameter
     */
    @NotNull
    public static Collection<TwigTypeContainer> resolveTwigMethodName(@NotNull PsiElement psiElement, @NotNull Collection<String> types) {
        if(types.isEmpty()) {
            return Collections.emptyList();
        }

        String rootType = types.iterator().next();
        Collection<PsiVariable> rootVariables = getRootVariableByName(psiElement, rootType);
        if (types.size() == 1) {
            Project project = psiElement.getProject();
            Collection<TwigTypeContainer> twigTypeContainers = TwigTypeContainer.fromCollection(rootVariables);
            for(TwigTypeResolver twigTypeResolver: TWIG_TYPE_RESOLVERS) {
                twigTypeResolver.resolve(project, twigTypeContainers, twigTypeContainers, rootType, new ArrayList<>(), rootVariables);
            }

            return twigTypeContainers;
        }

        Project project = psiElement.getProject();
        Collection<TwigTypeContainer> type = TwigTypeContainer.fromCollection(rootVariables);
        Collection<List<TwigTypeContainer>> previousElements = new ArrayList<>();
        previousElements.add(new ArrayList<>(type));

        String[] typeNames = types.toArray(new String[0]);
        for (int i = 1; i <= typeNames.length - 1; i++ ) {
            type = resolveTwigMethodName(project, type, typeNames[i], previousElements);
            previousElements.add(new ArrayList<>(type));

            // we can stop on empty list
            if(type.isEmpty()) {
                return Collections.emptyList();
            }
        }

        return type;
    }

    /**
     * Find scope related inline @var docs
     *
     * "@var foo \Foo"
     */
    private static Map<String, String> findInlineStatementVariableDocBlock(@NotNull PsiElement psiInsideBlock, @NotNull IElementType parentStatement, boolean nextParent) {
        PsiElement twigCompositeElement = PsiTreeUtil.findFirstParent(psiInsideBlock, psiElement ->
            PlatformPatterns.psiElement(parentStatement).accepts(psiElement)
        );

        Map<String, String> variables = new HashMap<>();
        if(twigCompositeElement == null) {
            return variables;
        }

        Map<String, String> inlineCommentDocsVars = new HashMap<>() {{
            putAll(getInlineCommentDocsVars(twigCompositeElement));
            putAll(getTypesTagVars(twigCompositeElement));
        }};

        // visit parent elements for extending scope
        if(nextParent) {
            PsiElement parent = twigCompositeElement.getParent();
            if(parent != null) {
                inlineCommentDocsVars.putAll(findInlineStatementVariableDocBlock(twigCompositeElement.getParent(), parentStatement, true));
            }
        }

        return inlineCommentDocsVars;
    }

    /**
     * Find file related doc blocks or "types" tags:
     *
     * - "@var foo \Foo"
     * - "{% types {...} %}"
     */
    public static Map<String, String> findFileVariableDocBlock(@NotNull TwigFile twigFile) {
        return new HashMap<>(CachedValuesManager.getCachedValue(
            twigFile,
            FILE_VARIABLE_DOC_BLOCK_CACHE,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableMap(findFileVariableDocBlockInner(twigFile)),
                twigFile
            )
        ));
    }

    @NotNull
    private static Map<String, String> findFileVariableDocBlockInner(@NotNull TwigFile twigFile) {
        return new HashMap<>() {{
            putAll(getInlineCommentDocsVars(twigFile));
            putAll(getTypesTagVars(twigFile));
        }};
    }

    /**
     * "@var foo \Foo"
     */
    private static Map<String, String> getInlineCommentDocsVars(@NotNull PsiElement twigCompositeElement) {
        Map<String, String> variables = new HashMap<>();

        for(PsiElement psiComment: YamlHelper.getChildrenFix(twigCompositeElement)) {
            if(!(psiComment instanceof PsiComment)) {
                continue;
            }

            String text = psiComment.getText();
            if(StringUtils.isBlank(text)) {
                continue;
            }

            for (Pattern pattern : INLINE_DOC_REGEX) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    variables.put(matcher.group("var"), matcher.group("class"));
                }
            }
        }

        return variables;
    }

    /**
     * {% types {...} %}
     */
    private static Map<String, String> getTypesTagVars(@NotNull PsiElement twigFile) {
        Map<String, String> variables = new HashMap<>();

        for (PsiElement psiComment: YamlHelper.getChildrenFix(twigFile)) {
            if (!(psiComment instanceof TwigCompositeElement) || psiComment.getNode().getElementType() != TwigElementTypes.TAG) {
                continue;
            }

            PsiElement firstChild = psiComment.getFirstChild();
            if (firstChild == null) {
                continue;
            }

            PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(firstChild, TwigTokenTypes.TAG_NAME);
            if (tagName == null || !"types".equals(tagName.getText())) {
                continue;
            }

            ASTNode lbraceCurlPsi = FormatterUtil.getNextNonWhitespaceLeaf(tagName.getNode());
            if (lbraceCurlPsi == null || lbraceCurlPsi.getElementType() != TwigTokenTypes.LBRACE_CURL) {
                continue;
            }

            ASTNode variableNamePsi = FormatterUtil.getNextNonWhitespaceLeaf(lbraceCurlPsi);
            if (variableNamePsi == null) {
                continue;
            }

            if (variableNamePsi.getElementType() == TwigTokenTypes.IDENTIFIER) {
                String variableName = variableNamePsi.getText();
                if (!variableName.isBlank()) {
                    variables.put(variableName, getTypesTagVarValue(variableNamePsi.getPsi()));
                }
            }

            for (PsiElement commaPsi : PsiElementUtils.getNextSiblingOfTypes(variableNamePsi.getPsi(), PlatformPatterns.psiElement().withElementType(TwigTokenTypes.COMMA))) {
                ASTNode commaPsiNext = FormatterUtil.getNextNonWhitespaceLeaf(commaPsi.getNode());
                if (commaPsiNext != null && commaPsiNext.getElementType() == TwigTokenTypes.IDENTIFIER) {
                    String variableName = commaPsiNext.getText();
                    if (!variableName.isBlank()) {
                        variables.put(variableName, getTypesTagVarValue(commaPsiNext.getPsi()));
                    }
                }
            }
        }

        return variables;
    }

    /**
     * Find value tarting scope key:
     * - : 'foo'
     * - : "foo"
     */
    @Nullable
    private static String getTypesTagVarValue(@NotNull PsiElement psiColon) {
        PsiElement filter = PsiElementUtils.getNextSiblingAndSkip(psiColon, TwigTokenTypes.STRING_TEXT, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.COLON, TwigTokenTypes.DOUBLE_QUOTE, TwigTokenTypes.QUESTION);
        if (filter == null) {
            return null;
        }

        String type = PsiElementUtils.trimQuote(filter.getText());
        if (type.isBlank()) {
            return null;
        }

        // secure value
        Matcher matcher = Pattern.compile("^(?<class>[\\w\\\\\\[\\]]+)$").matcher(type);
        if (matcher.find()) {
            // unescape: see also for Twig 4: https://github.com/twigphp/Twig/pull/4199
            return matcher.group("class").replace("\\\\", "\\");
        }

        // unknown
        return "\\mixed";
    }

    @NotNull
    public static Map<String, PsiVariable> collectScopeVariables(@NotNull PsiElement psiElement) {
        return collectScopeVariables(psiElement, new HashSet<>());
    }

    @NotNull
    public static Map<String, PsiVariable> collectScopeVariables(@NotNull PsiElement psiElement, @NotNull Set<VirtualFile> visitedFiles) {
        PsiFile containingFile = psiElement.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (visitedFiles.contains(virtualFile)) {
            return Collections.emptyMap();
        }

        visitedFiles.add(virtualFile);

        Map<String, VariableData> controllerVars = new HashMap<>();

        TwigFileVariableCollectorParameter collectorParameter = new TwigFileVariableCollectorParameter(psiElement, containingFile, visitedFiles);
        for (TwigFileVariableCollector collector: TWIG_FILE_VARIABLE_COLLECTORS.getExtensions()) {
            Map<String, Set<String>> globalVarsScope = new HashMap<>();
            collector.collect(collectorParameter, globalVarsScope);

            // @TODO: resolve this in change extension point, so that its only possible to provide data and dont give full scope to break / overwrite other variables
            globalVarsScope.forEach((s, strings) -> controllerVars.merge(s, VariableData.fromTypes(strings), VariableData::merge));

            // merging elements
            Map<String, PsiVariable> controllerVars1 = new HashMap<>();
            collector.collectPsiVariables(collectorParameter, controllerVars1);

            controllerVars1.forEach((s, psiVariable) -> controllerVars.merge(s, VariableData.fromPsiVariable(psiVariable), VariableData::merge));
        }

        // globals first @var first
        Collection<Map<String, String>> vars = Arrays.asList(
            findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.BLOCK_STATEMENT, true),
            findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.MACRO_STATEMENT, false),
            findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.FOR_STATEMENT, false)
        );

        // Inline Twig docs only provide type strings, e.g. "{# @var form \Symfony\Component\Form\FormView #}".
        for (Map<String, String> entry : vars) {
            entry.forEach((s, s2) -> controllerVars.merge(s, VariableData.fromType(s2), VariableData::merge));
        }

        // collect iterator
        controllerVars.replaceAll((s, psiVariable) -> psiVariable.withTypes(collectIteratorReturns(psiElement, psiVariable.types())));

        // check if we are in "for" scope and resolve types ending with []
        collectForArrayScopeVariables(psiElement, controllerVars);

        Map<String, PsiVariable> result = new HashMap<>();
        controllerVars.forEach((s, variableData) -> result.put(s, variableData.toPsiVariable()));

        return result;
    }

    /**
     * Extract magic iterator implementation like "getIterator" or "__iterator"
     *
     * "@TODO find core stuff for resolve possible class return values"
     *
     * "getIterator", "@method Foo __iterator", "@method Foo[] __iterator"
     */
    @NotNull
    private static Set<String> collectIteratorReturns(@NotNull PsiElement psiElement, @NotNull Set<String> types) {
        Set<String> arrayValues = new HashSet<>();
        for (String type : types) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), type);

            if(phpClass == null) {
                continue;
            }

            for (String methodName : new String[]{"getIterator", "__iterator", "current"}) {
                Method method = phpClass.findMethodByName(methodName);
                if(method != null) {
                    // @method Foo __iterator
                    // @method Foo[] __iterator
                    Set<String> iteratorTypes = method.getType().getTypes();
                    if("__iterator".equals(methodName) || "current".equals(methodName)) {
                        arrayValues.addAll(iteratorTypes.stream().map(x ->
                            !x.endsWith("[]") ? x + "[]" : x
                        ).collect(Collectors.toSet()));
                    } else {
                        // Foobar[]
                        for (String iteratorType : iteratorTypes) {
                            if(iteratorType.endsWith("[]")) {
                                arrayValues.add(iteratorType);
                            }
                        }
                    }
                }
            }
        }

        return arrayValues;
    }

    @NotNull
    private static Collection<String> collectForArrayScopeVariablesFoo(@NotNull Project project, @NotNull Collection<String> typeName, @NotNull VariableData variableData) {
        Collection<String> previousElements = variableData.types();

        String[] strings = typeName.toArray(new String[0]);

        for (int i = 1; i <= strings.length - 1; i++ ) {
            previousElements = resolveTwigMethodName(project, previousElements, strings[i]);

            // we can stop on empty list
            if(previousElements.isEmpty()) {
                return Collections.emptyList();
            }
        }

        return previousElements;
    }

    private static void collectForArrayScopeVariables(@NotNull PsiElement psiElement, @NotNull Map<String, VariableData> globalVars) {
        PsiElement twigCompositeElement = PsiTreeUtil.findFirstParent(psiElement, psiElement1 -> {
            if (psiElement1 instanceof TwigCompositeElement) {
                return PlatformPatterns.psiElement(TwigElementTypes.FOR_STATEMENT).accepts(psiElement1);
            }
            return false;
        });

        if(!(twigCompositeElement instanceof TwigCompositeElement)) {
            return;
        }

        PsiElement forTag = twigCompositeElement.getFirstChild();
        Pair<String, List<String>> forTagScope = getForTagScope(forTag);
        if (forTagScope == null) {
            return;
        }

        PhpType phpType = new PhpType();
        List<String> forTagInIdentifierString = forTagScope.getSecond();

        // {% for coolBar in coolBars.foos %}
        if (forTagInIdentifierString.size() > 1) {
            // nested resolve
            String rootElement = forTagInIdentifierString.getFirst();
            if(globalVars.containsKey(rootElement)) {
                VariableData variableData = globalVars.get(rootElement);
                for (String arrayType : collectForArrayScopeVariablesFoo(psiElement.getProject(), forTagInIdentifierString, variableData)) {
                    phpType.add(arrayType);
                }
            }

        } else {
            String variableName = forTagInIdentifierString.getFirst();
            if(!globalVars.containsKey(variableName)) {
                return;
            }

            // add single "for" var
            for (String s : globalVars.get(variableName).types()) {
                phpType.add(s);
            }
        }

        // find array types; since they are phptypes they ends with []
        Set<String> types = new HashSet<>();
        for(String arrayType: PhpIndex.getInstance(psiElement.getProject()).completeType(psiElement.getProject(), phpType, new HashSet<>()).getTypes()) {
            if(arrayType.endsWith("[]")) {
                types.add(arrayType.substring(0, arrayType.length() -2));
            }
        }

        // we already have same variable in scope, so merge types
        String scopeVariable = forTagScope.getFirst();
        globalVars.merge(scopeVariable, VariableData.fromTypes(types), VariableData::merge);
    }

    private record VariableData(@NotNull Set<String> types, @NotNull Set<String> formTypeFqns) {
        private VariableData {
            types = Set.copyOf(types);
            formTypeFqns = Set.copyOf(formTypeFqns);
        }

        @NotNull
        private static VariableData fromPsiVariable(@NotNull PsiVariable psiVariable) {
            return new VariableData(psiVariable.getTypes(), psiVariable.getFormTypeFqns());
        }

        @NotNull
        private static VariableData fromTypes(@NotNull Collection<String> types) {
            return new VariableData(new HashSet<>(types), Collections.emptySet());
        }

        @NotNull
        private static VariableData fromType(@NotNull String type) {
            return fromTypes(Collections.singleton(type));
        }

        @NotNull
        private VariableData withTypes(@NotNull Collection<String> types) {
            return merge(fromTypes(types));
        }

        @NotNull
        private VariableData merge(@NotNull VariableData variableData) {
            Set<String> types = new HashSet<>(this.types);
            types.addAll(variableData.types);

            Set<String> formTypeFqns = new HashSet<>(this.formTypeFqns);
            formTypeFqns.addAll(variableData.formTypeFqns);

            return new VariableData(types, formTypeFqns);
        }

        @NotNull
        private PsiVariable toPsiVariable() {
            return new PsiVariable(types, formTypeFqns);
        }
    }

    @NotNull
    private static Collection<PsiVariable> getRootVariableByName(@NotNull PsiElement psiElement, @NotNull String variableName) {
        Collection<PsiVariable> phpNamedElements = new ArrayList<>();

        for(Map.Entry<String, PsiVariable> variable : collectScopeVariables(psiElement).entrySet()) {
            if(variable.getKey().equals(variableName)) {
                phpNamedElements.add(variable.getValue());
            }
        }

        return phpNamedElements;
    }

    private static Collection<TwigTypeContainer> resolveTwigMethodName(@NotNull Project project, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> twigTypeContainer) {

        ArrayList<TwigTypeContainer> phpNamedElements = new ArrayList<>();

        for(TwigTypeContainer phpNamedElement: previousElement) {

            for(PhpNamedElement target : getTwigPhpNameTargets(project, phpNamedElement, typeName)) {
                PhpType phpType = target.getType();

                // @TODO: provide extension
                // custom resolving for Twig here: "app.user" => can also be a general solution just support the "getToken()->getUser()"
                if (target instanceof Method && StaticVariableCollector.isUserMethod((Method) target)) {
                    phpNamedElements.addAll(getApplicationUserImplementations(project));
                }

                Set<String> types = phpType.filterPrimitives().getTypes();
                if (!types.isEmpty()) {
                    phpNamedElements.add(new TwigTypeContainer(types));
                }
            }

            for(TwigTypeResolver twigTypeResolver: TWIG_TYPE_RESOLVERS) {
                twigTypeResolver.resolve(project, phpNamedElements, previousElement, typeName, twigTypeContainer, null);
            }

        }

        return phpNamedElements;
    }

    /**
     * Get possible suitable UserInterface implementation from the application scope
     */
    @NotNull
    private static Collection<TwigTypeContainer> getApplicationUserImplementations(@NotNull Project project) {
        return PhpIndexUtil
            .getAllSubclasses(project, "\\Symfony\\Component\\Security\\Core\\User\\UserInterface")
            .stream()
            .filter(phpClass -> !phpClass.isInterface()) // filter out implementation like AdvancedUserInterface
            .map(phpClass -> new TwigTypeContainer(Collections.singleton(phpClass.getFQN())))
            .collect(Collectors.toList());
    }

    private static Set<String> resolveTwigMethodName(Project project, Collection<String> previousElement, String typeName) {

        Set<String> types = new HashSet<>();

        for (PhpClass phpClass : PhpElementsUtil.getClassFromPhpTypeSet(project, new HashSet<>(previousElement))) {
            for(PhpNamedElement target : getTwigPhpNameTargets(phpClass, typeName)) {
                types.addAll(target.getType().getTypes());
            }
        }

        return types;
    }

    /**
     *
     * "phpNamedElement.variableName", "phpNamedElement.getVariableName" will resolve php type eg method
     *
     * @param phpNamedElement php class method or field
     * @param variableName variable name shortcut property possible
     * @return matched php types
     */
    public static Collection<? extends PhpNamedElement> getTwigPhpNameTargets(PhpNamedElement phpNamedElement, String variableName) {

        Collection<PhpNamedElement> targets = new ArrayList<>();
        if(phpNamedElement instanceof PhpClass) {

            for(Method method: ((PhpClass) phpNamedElement).getMethods()) {
                String methodName = method.getName();
                if(method.getModifier().isPublic() && (methodName.equalsIgnoreCase(variableName) || isPropertyShortcutMethodEqual(methodName, variableName))) {
                    targets.add(method);
                }
            }

            for(Field field: ((PhpClass) phpNamedElement).getFields()) {
                String fieldName = field.getName();
                if(field.getModifier().isPublic() && fieldName.equalsIgnoreCase(variableName)) {
                    targets.add(field);
                }
            }

        }

        return targets;
    }

    @NotNull
    public static Collection<PhpClass> resolveTwigTypeClasses(@NotNull Project project, @NotNull TwigTypeContainer twigTypeContainer) {
        if (twigTypeContainer.getTypes().isEmpty()) {
            return Collections.emptyList();
        }

        return PhpElementsUtil.getClassFromPhpTypeSet(project, twigTypeContainer.getTypes());
    }

    @NotNull
    public static Collection<? extends PhpNamedElement> getTwigPhpNameTargets(@NotNull Project project, @NotNull TwigTypeContainer twigTypeContainer, @NotNull String variableName) {
        Collection<PhpNamedElement> targets = new ArrayList<>();

        for (PhpClass phpClass : resolveTwigTypeClasses(project, twigTypeContainer)) {
            targets.addAll(getTwigPhpNameTargets(phpClass, variableName));
        }

        return targets;
    }


    public static String getTypeDisplayName(Project project, Set<String> types) {

        Collection<PhpClass> classFromPhpTypeSet = PhpElementsUtil.getClassFromPhpTypeSet(project, types);
        if(!classFromPhpTypeSet.isEmpty()) {
            return classFromPhpTypeSet.iterator().next().getPresentableFQN();
        }

        PhpType phpType = new PhpType();
        for (String type : types) {
            phpType.add(type);
        }
        PhpType phpTypeFormatted = PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<>());

        if(!phpTypeFormatted.getTypes().isEmpty()) {
            return StringUtils.join(phpTypeFormatted.getTypes(), "|");
        }

        if(!types.isEmpty()) {
            return types.iterator().next();
        }

        return "";

    }

    public static boolean isPropertyShortcutMethod(Method method) {
        return isPropertyShortcutMethod(method.getName());
    }

    /**
     * Checks if a method is accessible as a Twig property/method.
     * Methods are accessible if they are:
     * - public
     * - not starting with "set" (setters are not exposed in Twig)
     * - not starting with "__" (magic methods)
     *
     * @param method The method to check
     * @return true if the method is accessible in Twig templates
     */
    public static boolean isTwigAccessibleMethod(@NotNull Method method) {
        // Early exit on non-public - avoids getName() call for most methods
        if (!method.getModifier().isPublic()) {
            return false;
        }

        String name = method.getName();
        return !name.startsWith("set") && !name.startsWith("__");
    }

    public static boolean isWeakCollectionLikeClass(@NotNull PhpClass phpClass) {
        return PhpElementsUtil.isInstanceOf(phpClass, "ArrayAccess") || PhpElementsUtil.isInstanceOf(phpClass, "Iterator");
    }

    public static boolean isPropertyShortcutMethod(String methodName) {
        for (String shortcut: PROPERTY_SHORTCUTS) {
            if (methodName.startsWith(shortcut) && methodName.length() > shortcut.length()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPropertyShortcutMethodEqual(String methodName, String variableName) {
        for (String shortcut: PROPERTY_SHORTCUTS) {
            if (methodName.equalsIgnoreCase(shortcut + variableName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts the loop variable and iterable path from a Twig for tag.
     *
     * {% for entry in root.children.entries %}
     * => ("entry", ["root", "children", "entries"])
     */
    @Nullable
    public static Pair<String, List<String>> getForTagScope(@Nullable PsiElement forTag) {
        if (forTag == null || forTag.getNode().getElementType() != TwigElementTypes.FOR_TAG) {
            return null;
        }

        PsiElement forScopeVariable = PsiElementUtils.getChildrenOfType(forTag, TwigPattern.getForTagVariablePattern());
        if (forScopeVariable == null) {
            return null;
        }

        List<String> path = new ArrayList<>(getForTagIdentifierAsString(forTag));
        if (path.isEmpty()) {
            return null;
        }

        return Pair.create(forScopeVariable.getText(), Collections.unmodifiableList(path));
    }

    /**
     * Twig attribute shortcuts
     *
     * getFoo => foo
     * hasFoo => foo
     * isFoo => foo
     */
    @NotNull
    public static String getPropertyShortcutMethodName(@NotNull Method method) {
        return getPropertyShortcutMethodName(method.getName());
    }

    public static String getPropertyShortcutMethodName(@NotNull String methodName) {
        for (String shortcut: PROPERTY_SHORTCUTS) {
            // strip possible property shortcut and make it lcfirst
            if (methodName.startsWith(shortcut) && methodName.length() > shortcut.length()) {
                methodName = methodName.substring(shortcut.length());
                return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
            }
        }

        return methodName;
    }

    /**
     * Get the "for IN" variable identifier as separated string
     *
     *  {% for car in "cars" %}
     *  {% for car in "cars"|length %}
     *  {% for car in "cars.test" %}
     */
    @NotNull
    private static Collection<String> getForTagIdentifierAsString(PsiElement forTag) {
        if(forTag.getNode().getElementType() != TwigElementTypes.FOR_TAG) {
            return Collections.emptyList();
        }

        // getChildren hack
        PsiElement firstChild = forTag.getFirstChild();
        if(firstChild == null) {
            return Collections.emptyList();
        }

        // find IN token
        PsiElement psiIn = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.IN));
        if(psiIn == null) {
            return Collections.emptyList();
        }

        // find next IDENTIFIER, eg skip whitespaces
        PsiElement psiIdentifier = PsiElementUtils.getNextSiblingOfType(psiIn, captureVariableOrField());
        if(psiIdentifier == null) {
            return Collections.emptyList();
        }

        // find non common token type. we only allow: "test.test"
        PsiElement afterInVarPsiElement = PsiElementUtils.getNextSiblingOfType(psiIdentifier, PlatformPatterns.psiElement().andNot(PlatformPatterns.or(
            PlatformPatterns.psiElement((TwigTokenTypes.IDENTIFIER)),
            PlatformPatterns.psiElement((TwigTokenTypes.DOT))
        )));

        if(afterInVarPsiElement == null) {
            return Collections.emptyList();
        }

        return TwigTypeResolveUtil.formatPsiTypeName(afterInVarPsiElement);
    }
}
