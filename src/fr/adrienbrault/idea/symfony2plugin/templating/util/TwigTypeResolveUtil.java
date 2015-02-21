package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
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
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.*;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormVarsResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.TwigTypeResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigTypeResolveUtil {

    public static final String DOC_PATTERN  = "\\{#[\\s]+([\\w]+)[\\s]+([\\w\\\\\\[\\]]+)[\\s]+#}";
    public static final String DOC_PATTERN_2  = "\\{#[\\s]+@var[\\s]+([\\w]+)[\\s]+([\\w\\\\\\[\\]]+)[\\s]+#}";
    private static String[] propertyShortcuts = new String[] {"get", "is"};

    private static TwigFileVariableCollector[] twigFileVariableCollectors = new TwigFileVariableCollector[] {
        new StaticVariableCollector(),
        new GlobalExtensionVariableCollector(),
        new ControllerDocVariableCollector(),
        new ServiceContainerVariableCollector(),
        new FileDocVariableCollector(),
        new ControllerVariableCollector(),
        new IncludeVariableCollector()
    };

    private static TwigTypeResolver[] twigTypeResolvers = new TwigTypeResolver[] {
        new FormVarsResolver(),
        new FormFieldResolver(),
    };

    public static String[] formatPsiTypeName(PsiElement psiElement, boolean includeCurrent) {
        ArrayList<String> strings = new ArrayList<String>(Arrays.asList(formatPsiTypeName(psiElement)));
        strings.add(psiElement.getText());
        return strings.toArray(new String[strings.size()]);
    }

    public static String[] formatPsiTypeName(PsiElement psiElement) {

        String typeNames = PhpElementsUtil.getPrevSiblingAsTextUntil(psiElement, PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
            PlatformPatterns.psiElement(PsiWhiteSpace.class
        )));

        if(typeNames.trim().length() == 0) {
            return new String[]{};
        }

        if(typeNames.endsWith(".")) {
            typeNames = typeNames.substring(0, typeNames.length() -1);
        }

        String[] possibleTypes;
        if(typeNames.contains(".")) {
            possibleTypes = typeNames.split("\\.");
        } else {
            possibleTypes = new String[]{typeNames};
        }

        return possibleTypes;
    }

    public static Collection<TwigTypeContainer> resolveTwigMethodName(PsiElement psiElement, String[] typeName) {

        if(typeName.length == 0) {
            return Collections.emptyList();
        }

        List<PsiVariable> rootVariables = getRootVariableByName(psiElement, typeName[0]);
        if(typeName.length == 1) {

            Collection<TwigTypeContainer> twigTypeContainers = TwigTypeContainer.fromCollection(psiElement.getProject(), rootVariables);
            for(TwigTypeResolver twigTypeResolver: twigTypeResolvers) {
                twigTypeResolver.resolve(twigTypeContainers, twigTypeContainers, typeName[0], new ArrayList<List<TwigTypeContainer>>(), rootVariables);
            }

            return twigTypeContainers;
        }

        Collection<TwigTypeContainer> type = TwigTypeContainer.fromCollection(psiElement.getProject(), rootVariables);
        Collection<List<TwigTypeContainer>> previousElements = new ArrayList<List<TwigTypeContainer>> ();
        previousElements.add(new ArrayList<TwigTypeContainer>(type));

        for (int i = 1; i <= typeName.length - 1; i++ ) {
            type = resolveTwigMethodName(type, typeName[i], previousElements);
            previousElements.add(new ArrayList<TwigTypeContainer>(type));

            // we can stop on empty list
            if(type.size() == 0) {
                return Collections.emptyList();
            }

        }

        return type;
    }

    /**
     * duplicate use a collector interface
     */
    private static Map<String, String> findInlineStatementVariableDocBlock(PsiElement psiInsideBlock, final IElementType parentStatement) {

        PsiElement twigCompositeElement = PsiTreeUtil.findFirstParent(psiInsideBlock, new Condition<PsiElement>() {
            @Override
            public boolean value(PsiElement psiElement) {
                if (psiElement instanceof TwigCompositeElement) {
                    if (PlatformPatterns.psiElement(parentStatement).accepts(psiElement)) {
                        return true;
                    }
                }
                return false;
            }
        });

        Map<String, String> variables = new HashMap<String, String>();
        if(twigCompositeElement == null) {
            return variables;
        }

        // wtf in completion { | } root we have no comments in child context !?
        Pattern pattern = Pattern.compile(DOC_PATTERN);
        Pattern pattern2 = Pattern.compile(DOC_PATTERN_2);

        for(PsiElement psiComment: YamlHelper.getChildrenFix(twigCompositeElement)) {
            if(psiComment instanceof PsiComment) {

                Matcher matchVar = pattern2.matcher(psiComment.getText());
                if (matchVar.find()) {
                    variables.put(matchVar.group(1), matchVar.group(2));
                } else {
                    Matcher matchInline = pattern.matcher(psiComment.getText());
                    if (matchInline.find()) {
                        variables.put(matchInline.group(1), matchInline.group(2));
                    }
                }

            }
        }

        return variables;
    }

    /**
     * duplicate use a collector interface
     */
    public static Map<String, String> findFileVariableDocBlock(TwigFile twigFile) {

        Pattern pattern = Pattern.compile(DOC_PATTERN);
        Pattern pattern2 = Pattern.compile(DOC_PATTERN_2);

        // wtf in completion { | } root we have no comments in child context !?
        Map<String, String> variables = new HashMap<String, String>();
        for(PsiElement psiComment: YamlHelper.getChildrenFix(twigFile)) {
            if(psiComment instanceof PsiComment) {
                Matcher matcher = pattern.matcher(psiComment.getText());
                if (matcher.find()) {
                    variables.put(matcher.group(1), matcher.group(2));
                }
                matcher = pattern2.matcher(psiComment.getText());
                if (matcher.find()) {
                    variables.put(matcher.group(1), matcher.group(2));
                }
            }
        }

        return variables;
    }

    private static Map<String, Set<String>> convertHashMapToTypeSet(Map<String, String> hashMap) {
        Map<String, Set<String>> globalVars = new HashMap<String, Set<String>>();

        for(final Map.Entry<String, String> entry: hashMap.entrySet()) {
            globalVars.put(entry.getKey(), new HashSet<String>(Arrays.asList(entry.getValue())));
        }

        return globalVars;
    }

    @NotNull
    public static Map<String, PsiVariable> collectScopeVariables(@NotNull PsiElement psiElement) {
        return collectScopeVariables(psiElement, new HashSet<VirtualFile>());
    }

    @NotNull
    public static Map<String, PsiVariable> collectScopeVariables(@NotNull PsiElement psiElement, @NotNull Set<VirtualFile> visitedFiles) {

        Map<String, Set<String>> globalVars = new HashMap<String, Set<String>>();
        Map<String, PsiVariable> controllerVars = new HashMap<String, PsiVariable>();

        VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
        if(visitedFiles.contains(virtualFile)) {
            return controllerVars;
        }

        visitedFiles.add(virtualFile);

        TwigFileVariableCollectorParameter collectorParameter = new TwigFileVariableCollectorParameter(psiElement, visitedFiles);
        for(TwigFileVariableCollector collector: twigFileVariableCollectors) {
            collector.collect(collectorParameter, globalVars);

            if(collector instanceof TwigFileVariableCollector.TwigFileVariableCollectorExt) {
                ((TwigFileVariableCollector.TwigFileVariableCollectorExt) collector).collectVars(collectorParameter, controllerVars);
            }

        }

        // globals first
        globalVars.putAll(convertHashMapToTypeSet(findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.BLOCK_STATEMENT)));
        globalVars.putAll(convertHashMapToTypeSet(findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.MACRO_STATEMENT)));
        globalVars.putAll(convertHashMapToTypeSet(findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.FOR_STATEMENT)));

        for(Map.Entry<String, Set<String>> entry: globalVars.entrySet()) {
            controllerVars.put(entry.getKey(), new PsiVariable(entry.getValue(), null));
        }

        // check if we are in "for" scope and resolve types ending with []
        collectForArrayScopeVariables(psiElement, controllerVars);

        return controllerVars;
    }


    private static Collection<String> collectForArrayScopeVariablesFoo(Project project, String[] typeName, PsiVariable psiVariable) {

        Collection<String> previousElements = psiVariable.getTypes();

        for (int i = 1; i <= typeName.length - 1; i++ ) {

            previousElements = resolveTwigMethodName(project, previousElements, typeName[i]);

            // we can stop on empty list
            if(previousElements.size() == 0) {
                return Collections.emptyList();
            }

        }

        return previousElements;

    }

    private static void collectForArrayScopeVariables(PsiElement psiElement, Map<String, PsiVariable> globalVars) {

        PsiElement twigCompositeElement = PsiTreeUtil.findFirstParent(psiElement, new Condition<PsiElement>() {
            @Override
            public boolean value(PsiElement psiElement) {
                if (psiElement instanceof TwigCompositeElement) {
                    if (PlatformPatterns.psiElement(TwigElementTypes.FOR_STATEMENT).accepts(psiElement)) {
                        return true;
                    }
                }
                return false;
            }
        });

        if(!(twigCompositeElement instanceof TwigCompositeElement)) {
            return;
        }

        // {% for user in "users" %}
        PsiElement forTag = twigCompositeElement.getFirstChild();
        PsiElement inVariable = PsiElementUtils.getChildrenOfType(forTag, TwigHelper.getForTagInVariablePattern());
        if(inVariable == null) {
            return;
        }

        String variableName = inVariable.getText();
        if(!globalVars.containsKey(variableName)) {
            return;
        }

        // {% for "user" in users %}
        PsiElement forScopeVariable = PsiElementUtils.getChildrenOfType(forTag, TwigHelper.getForTagVariablePattern());
        if(forScopeVariable == null) {
            return;
        }

        PhpType phpType = new PhpType();

        // {% for coolBar in coolBars.foos %}
        Pattern pattern = Pattern.compile("[\\s]+([\\w\\.]+)[\\s]+");
        Matcher matcher = pattern.matcher(forTag.getText());
        if (matcher.find()) {

            PsiElement nextSiblingOfType = PsiElementUtils.getNextSiblingOfType(inVariable, PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ));

            // nested resolve
            if(nextSiblingOfType != null) {
                String[] typeName = TwigTypeResolveUtil.formatPsiTypeName(nextSiblingOfType);
                if(typeName.length > 1 && globalVars.containsKey(typeName[0])) {
                    PsiVariable psiVariable = globalVars.get(typeName[0]);
                    for (String arrayType : collectForArrayScopeVariablesFoo(psiElement.getProject(), typeName, psiVariable)) {
                        phpType.add(arrayType);
                    }
                }
            }

        } else {
            // add single "for" var
            phpType.add(globalVars.get(variableName).getTypes());
        }

        String scopeVariable = forScopeVariable.getText();

        // find array types; since they are phptypes they ends with []
        Set<String> types = new HashSet<String>();
        for(String arrayType: PhpIndex.getInstance(psiElement.getProject()).completeType(psiElement.getProject(), phpType, new HashSet<String>()).getTypes()) {
            if(arrayType.endsWith("[]")) {
                types.add(arrayType.substring(0, arrayType.length() -2));
            }
        }

        globalVars.put(scopeVariable, new PsiVariable(types));

    }

    private static List<PsiVariable> getRootVariableByName(PsiElement psiElement, String variableName) {

        List<PsiVariable> phpNamedElements = new ArrayList<PsiVariable>();
        for(Map.Entry<String, PsiVariable> variable : collectScopeVariables(psiElement).entrySet()) {
            if(variable.getKey().equals(variableName)) {
                phpNamedElements.add(variable.getValue());
                //phpNamedElements.addAll(PhpElementsUtil.getClassFromPhpTypeSet(psiElement.getProject(), variable.getValue().getTypes()));
            }

        }

        return phpNamedElements;

    }

    private static Collection<TwigTypeContainer> resolveTwigMethodName(Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> twigTypeContainer) {

        ArrayList<TwigTypeContainer> phpNamedElements = new ArrayList<TwigTypeContainer>();

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

            for(TwigTypeResolver twigTypeResolver: twigTypeResolvers) {
                twigTypeResolver.resolve(phpNamedElements, previousElement, typeName, twigTypeContainer, null);
            }

        }

        return phpNamedElements;
    }

    private static Set<String> resolveTwigMethodName(Project project, Collection<String> previousElement, String typeName) {

        Set<String> types = new HashSet<String>();

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

        Collection<PhpNamedElement> targets = new ArrayList<PhpNamedElement>();
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

        for(PhpClass phpClass: PhpElementsUtil.getClassFromPhpTypeSet(project, types)) {
            if(phpClass.getPresentableFQN() != null) {
                return phpClass.getPresentableFQN();
            }
        }

        PhpType phpType = new PhpType();
        phpType.add(types);
        PhpType phpTypeFormatted = PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<String>());

        if(phpTypeFormatted.getTypes().size() > 0) {
            return StringUtils.join(phpTypeFormatted.getTypes(), "|");
        }

        if(types.size() > 0) {
            return types.iterator().next();
        }

        return "";

    }

    public static boolean isPropertyShortcutMethod(Method method) {

        for(String shortcut: propertyShortcuts) {
            if(method.getName().startsWith(shortcut) && method.getName().length() > shortcut.length()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPropertyShortcutMethodEqual(String methodName, String variableName) {

        for(String shortcut: propertyShortcuts) {
            if(methodName.equalsIgnoreCase(shortcut + variableName)) {
                return true;
            }
        }

        return false;
    }

    public static String getPropertyShortcutMethodName(Method method) {

        String methodName = method.getName();
        for(String shortcut: propertyShortcuts) {
            // strip possible property shortcut and make it lcfirst
            if(method.getName().startsWith(shortcut) && method.getName().length() > shortcut.length()) {
                methodName = methodName.substring(shortcut.length());
                return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
            }
        }

        return methodName;
    }

}

