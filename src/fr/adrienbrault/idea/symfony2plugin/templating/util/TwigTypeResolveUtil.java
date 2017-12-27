package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
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
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormVarsResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.TwigTypeResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final String DOC_TYPE_PATTERN_CLASS_SECOND = "@var[\\s]+(?<var>[\\w]+)[\\s]+(?<class>[\\w\\\\\\[\\]]+)[\\s]*";

    /**
     * {# @var \AppBundle\Entity\Foo[] variable #}
     * {# @var \AppBundle\Entity\Foo variable #}
     */
    private static final String DOC_TYPE_PATTERN_CLASS_FIRST = "@var[\\s]+(?<class>[\\w\\\\\\[\\]]+)[\\s]+(?<var>[\\w]+)[\\s]*";

    public static final Pattern[] INLINE_DOC_REGEX = {
        Pattern.compile(DOC_TYPE_PATTERN_CLASS_SECOND, Pattern.MULTILINE),
        Pattern.compile(DOC_TYPE_PATTERN_CLASS_FIRST, Pattern.MULTILINE),
        Pattern.compile(DEPRECATED_DOC_TYPE_PATTERN),
    };

    // for supporting completion and navigation of one line element
    public static final String[] DOC_TYPE_PATTERN_SINGLE  = new String[] {
        "\\{#[\\s]+(?<var>[\\w]+)[\\s]+(?<class>[\\w\\\\\\[\\]]+)[\\s]+#}",
        "\\{#[\\s]+"+ DOC_TYPE_PATTERN_CLASS_SECOND + "[\\s]+#}",
        "\\{#[\\s]+"+ DOC_TYPE_PATTERN_CLASS_FIRST + "[\\s]+#}",
    };

    private static String[] PROPERTY_SHORTCUTS = new String[] {"get", "is", "has"};

    private static final ExtensionPointName<TwigFileVariableCollector> TWIG_FILE_VARIABLE_COLLECTORS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.TwigVariableCollector"
    );

    private static TwigTypeResolver[] TWIG_TYPE_RESOLVERS = new TwigTypeResolver[] {
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

        if(typeNames.trim().length() == 0) {
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
        if(types.size() == 0) {
            return Collections.emptyList();
        }

        String rootType = types.iterator().next();
        Collection<PsiVariable> rootVariables = getRootVariableByName(psiElement, rootType);
        if(types.size() == 1) {
            Collection<TwigTypeContainer> twigTypeContainers = TwigTypeContainer.fromCollection(psiElement.getProject(), rootVariables);
            for(TwigTypeResolver twigTypeResolver: TWIG_TYPE_RESOLVERS) {
                twigTypeResolver.resolve(twigTypeContainers, twigTypeContainers, rootType, new ArrayList<>(), rootVariables);
            }

            return twigTypeContainers;
        }

        Collection<TwigTypeContainer> type = TwigTypeContainer.fromCollection(psiElement.getProject(), rootVariables);
        Collection<List<TwigTypeContainer>> previousElements = new ArrayList<>();
        previousElements.add(new ArrayList<>(type));

        String[] typeNames = types.toArray(new String[types.size()]);
        for (int i = 1; i <= typeNames.length - 1; i++ ) {
            type = resolveTwigMethodName(type, typeNames[i], previousElements);
            previousElements.add(new ArrayList<>(type));

            // we can stop on empty list
            if(type.size() == 0) {
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

        Map<String, String> inlineCommentDocsVars = getInlineCommentDocsVars(twigCompositeElement);

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
     * Find file related doc blocks:
     *
     * "@var foo \Foo"
     */
    public static Map<String, String> findFileVariableDocBlock(@NotNull TwigFile twigFile) {
        return getInlineCommentDocsVars(twigFile);
    }

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

    private static Map<String, Set<String>> convertHashMapToTypeSet(Map<String, String> hashMap) {
        Map<String, Set<String>> globalVars = new HashMap<>();

        for(final Map.Entry<String, String> entry: hashMap.entrySet()) {
            globalVars.put(entry.getKey(), new HashSet<>(Collections.singletonList(entry.getValue())));
        }

        return globalVars;
    }

    @NotNull
    public static Map<String, PsiVariable> collectScopeVariables(@NotNull PsiElement psiElement) {
        return collectScopeVariables(psiElement, new HashSet<>());
    }

    @NotNull
    public static Map<String, PsiVariable> collectScopeVariables(@NotNull PsiElement psiElement, @NotNull Set<VirtualFile> visitedFiles) {
        Map<String, Set<String>> globalVars = new HashMap<>();
        Map<String, PsiVariable> controllerVars = new HashMap<>();

        VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
        if(visitedFiles.contains(virtualFile)) {
            return controllerVars;
        }

        visitedFiles.add(virtualFile);

        TwigFileVariableCollectorParameter collectorParameter = new TwigFileVariableCollectorParameter(psiElement, visitedFiles);
        for(TwigFileVariableCollector collector: TWIG_FILE_VARIABLE_COLLECTORS.getExtensions()) {
            collector.collect(collectorParameter, globalVars);
            collector.collectPsiVariables(collectorParameter, controllerVars);
        }

        // globals first
        globalVars.putAll(convertHashMapToTypeSet(findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.BLOCK_STATEMENT, true)));
        globalVars.putAll(convertHashMapToTypeSet(findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.MACRO_STATEMENT, false)));
        globalVars.putAll(convertHashMapToTypeSet(findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.FOR_STATEMENT, false)));

        for(Map.Entry<String, Set<String>> entry: globalVars.entrySet()) {
            Set<String> types = entry.getValue();

            // collect iterator
            types.addAll(collectIteratorReturns(psiElement, entry.getValue()));

            // convert to variable model
            controllerVars.put(entry.getKey(), new PsiVariable(types, null));
        }

        // check if we are in "for" scope and resolve types ending with []
        collectForArrayScopeVariables(psiElement, controllerVars);

        return controllerVars;
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
    private static Collection<String> collectForArrayScopeVariablesFoo(@NotNull Project project, @NotNull Collection<String> typeName, @NotNull PsiVariable psiVariable) {
        Collection<String> previousElements = psiVariable.getTypes();

        String[] strings = typeName.toArray(new String[typeName.size()]);

        for (int i = 1; i <= strings.length - 1; i++ ) {
            previousElements = resolveTwigMethodName(project, previousElements, strings[i]);

            // we can stop on empty list
            if(previousElements.size() == 0) {
                return Collections.emptyList();
            }
        }

        return previousElements;
    }

    private static void collectForArrayScopeVariables(PsiElement psiElement, Map<String, PsiVariable> globalVars) {
        PsiElement twigCompositeElement = PsiTreeUtil.findFirstParent(psiElement, psiElement1 -> {
            if (psiElement1 instanceof TwigCompositeElement) {
                if (PlatformPatterns.psiElement(TwigElementTypes.FOR_STATEMENT).accepts(psiElement1)) {
                    return true;
                }
            }
            return false;
        });

        if(!(twigCompositeElement instanceof TwigCompositeElement)) {
            return;
        }

        // {% for user in "users" %}
        PsiElement forTag = twigCompositeElement.getFirstChild();
        PsiElement inVariable = PsiElementUtils.getChildrenOfType(forTag, TwigPattern.getForTagInVariablePattern());
        if(inVariable == null) {
            return;
        }

        String variableName = inVariable.getText();
        if(!globalVars.containsKey(variableName)) {
            return;
        }

        // {% for "user" in users %}
        PsiElement forScopeVariable = PsiElementUtils.getChildrenOfType(forTag, TwigPattern.getForTagVariablePattern());
        if(forScopeVariable == null) {
            return;
        }

        PhpType phpType = new PhpType();

        Collection<String> forTagInIdentifierString = getForTagIdentifierAsString(forTag);
        // {% for coolBar in coolBars.foos %}
        if (forTagInIdentifierString.size() > 1) {

            // nested resolve
            String rootElement = forTagInIdentifierString.iterator().next();
            if(globalVars.containsKey(rootElement)) {
                PsiVariable psiVariable = globalVars.get(rootElement);
                for (String arrayType : collectForArrayScopeVariablesFoo(psiElement.getProject(), forTagInIdentifierString, psiVariable)) {
                    phpType.add(arrayType);
                }
            }

        } else {
            // add single "for" var
            for (String s : globalVars.get(variableName).getTypes()) {
                phpType.add(s);
            }
        }

        String scopeVariable = forScopeVariable.getText();

        // find array types; since they are phptypes they ends with []
        Set<String> types = new HashSet<>();
        for(String arrayType: PhpIndex.getInstance(psiElement.getProject()).completeType(psiElement.getProject(), phpType, new HashSet<>()).getTypes()) {
            if(arrayType.endsWith("[]")) {
                types.add(arrayType.substring(0, arrayType.length() -2));
            }
        }

        // we already have same variable in scope, so merge types
        if(globalVars.containsKey(scopeVariable)) {
            globalVars.get(scopeVariable).getTypes().addAll(types);
        } else {
            globalVars.put(scopeVariable, new PsiVariable(types));
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

    private static Collection<TwigTypeContainer> resolveTwigMethodName(Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> twigTypeContainer) {

        ArrayList<TwigTypeContainer> phpNamedElements = new ArrayList<>();

        for(TwigTypeContainer phpNamedElement: previousElement) {

            if(phpNamedElement.getPhpNamedElement() != null) {
                for(PhpNamedElement target : getTwigPhpNameTargets(phpNamedElement.getPhpNamedElement(), typeName)) {
                    PhpType phpType = target.getType();
                    for(String typeString: phpType.getTypes()) {
                        PhpNamedElement phpNamedElement1 = PhpElementsUtil.getClassInterface(phpNamedElement.getPhpNamedElement().getProject(), typeString);
                        if(phpNamedElement1 != null) {
                            phpNamedElements.add(new TwigTypeContainer(phpNamedElement1));
                        }
                    }
                }
            }

            for(TwigTypeResolver twigTypeResolver: TWIG_TYPE_RESOLVERS) {
                twigTypeResolver.resolve(phpNamedElements, previousElement, typeName, twigTypeContainer, null);
            }

        }

        return phpNamedElements;
    }

    private static Set<String> resolveTwigMethodName(Project project, Collection<String> previousElement, String typeName) {

        Set<String> types = new HashSet<>();

        for(String prevClass: previousElement) {
            for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(project, prevClass)) {
                for(PhpNamedElement target : getTwigPhpNameTargets(phpClass, typeName)) {
                    types.addAll(target.getType().getTypes());
                }
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


    public static String getTypeDisplayName(Project project, Set<String> types) {

        Collection<PhpClass> classFromPhpTypeSet = PhpElementsUtil.getClassFromPhpTypeSet(project, types);
        if(classFromPhpTypeSet.size() > 0) {
            return classFromPhpTypeSet.iterator().next().getPresentableFQN();
        }

        PhpType phpType = new PhpType();
        for (String type : types) {
            phpType.add(type);
        }
        PhpType phpTypeFormatted = PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<>());

        if(phpTypeFormatted.getTypes().size() > 0) {
            return StringUtils.join(phpTypeFormatted.getTypes(), "|");
        }

        if(types.size() > 0) {
            return types.iterator().next();
        }

        return "";

    }

    public static boolean isPropertyShortcutMethod(Method method) {

        for(String shortcut: PROPERTY_SHORTCUTS) {
            if(method.getName().startsWith(shortcut) && method.getName().length() > shortcut.length()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPropertyShortcutMethodEqual(String methodName, String variableName) {

        for(String shortcut: PROPERTY_SHORTCUTS) {
            if(methodName.equalsIgnoreCase(shortcut + variableName)) {
                return true;
            }
        }

        return false;
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
        String methodName = method.getName();

        for(String shortcut: PROPERTY_SHORTCUTS) {
            // strip possible property shortcut and make it lcfirst
            if(method.getName().startsWith(shortcut) && method.getName().length() > shortcut.length()) {
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
        PsiElement psiIdentifier = PsiElementUtils.getNextSiblingOfType(psiIn, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER));
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

