package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
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
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.*;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigTypeResolveUtil {

    public static final String DOC_PATTERN  = "\\{#[\\s]+([\\w]+)[\\s]+([\\w\\\\\\[\\]]+)[\\s]+#}";

    private static TwigFileVariableCollector[] twigFileVariableCollectors = new TwigFileVariableCollector[] {
        new StaticVariableCollector(),
        new GlobalExtensionVariableCollector(),
        new ControllerDocVariableCollector(),
        new ServiceContainerVariableCollector(),
        new FileDocVariableCollector(),
        new ControllerVariableCollector(),
    };

    public static String[] formatPsiTypeName(PsiElement psiElement, boolean includeCurrent) {
        ArrayList<String> strings = new ArrayList<String>(Arrays.asList(formatPsiTypeName(psiElement)));
        strings.add(psiElement.getText());
        return strings.toArray(new String[strings.size()]);
    }

    public static String[] formatPsiTypeName(PsiElement psiElement) {

        String typeNames = PhpElementsUtil.getPrevSiblingAsTextUntil(psiElement, PlatformPatterns.psiElement(PsiWhiteSpace.class));
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

    public static Collection<? extends PhpNamedElement> resolveTwigMethodName(PsiElement psiElement, String[] typeName) {

        if(typeName.length == 0) {
            return Collections.emptyList();
        }

        Collection<? extends PhpNamedElement> rootVariable = getRootVariableByName(psiElement, typeName[0]);
        if(typeName.length == 1) {
            return rootVariable;
        }

        Collection<? extends PhpNamedElement> type = rootVariable;
        for (int i = 1; i <= typeName.length - 1; i++ ) {
            type = resolveTwigMethodName(type, typeName[i]);

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
    private static HashMap<String, String> findInlineStatementVariableDocBlock(PsiElement psiInsideBlock, final IElementType parentStatement) {

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

        HashMap<String, String> variables = new HashMap<String, String>();
        if(twigCompositeElement == null) {
            return variables;
        }

        // wtf in completion { | } root we have no comments in child context !?
        Pattern pattern = Pattern.compile(DOC_PATTERN);
        for(PsiElement psiComment: YamlHelper.getChildrenFix(twigCompositeElement)) {
            if(psiComment instanceof PsiComment) {
                Matcher matcher = pattern.matcher(psiComment.getText());
                if (matcher.find()) {
                    variables.put(matcher.group(1), matcher.group(2));
                }
            }
        }

        return variables;
    }

    /**
     * duplicate use a collector interface
     */
    public static HashMap<String, String> findFileVariableDocBlock(TwigFile twigFile) {

        Pattern pattern = Pattern.compile(DOC_PATTERN);

        // wtf in completion { | } root we have no comments in child context !?
        HashMap<String, String> variables = new HashMap<String, String>();
        for(PsiElement psiComment: YamlHelper.getChildrenFix(twigFile)) {
            if(psiComment instanceof PsiComment) {
                Matcher matcher = pattern.matcher(psiComment.getText());
                if (matcher.find()) {
                    variables.put(matcher.group(1), matcher.group(2));
                }
            }
        }

        return variables;
    }

    private static HashMap<String, Set<String>> convertHashMapToTypeSet(HashMap<String, String> hashMap) {
        HashMap<String, Set<String>> globalVars = new HashMap<String, Set<String>>();

        for(final Map.Entry<String, String> entry: hashMap.entrySet()) {
            globalVars.put(entry.getKey(), new HashSet<String>(Arrays.asList(entry.getValue())));
        }

        return globalVars;
    }

    public static HashMap<String, Set<String>> collectScopeVariables(PsiElement psiElement) {

        HashMap<String, Set<String>> globalVars = new HashMap<String, Set<String>>();

        TwigFileVariableCollectorParameter collectorParameter = new TwigFileVariableCollectorParameter(psiElement);
        for(TwigFileVariableCollector collector: twigFileVariableCollectors) {
            collector.collect(collectorParameter, globalVars);
        }

        // globals first
        globalVars.putAll(convertHashMapToTypeSet(findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.BLOCK_STATEMENT)));
        globalVars.putAll(convertHashMapToTypeSet(findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.FOR_STATEMENT)));

        // check if we are in "for" scope and resolve types ending with []
        collectForArrayScopeVariables(psiElement, globalVars);

        return globalVars;
    }

    private static void collectForArrayScopeVariables(PsiElement psiElement, HashMap<String, Set<String>> globalVars) {

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

        String scopeVariable = forScopeVariable.getText();

        // find array types; since they are phptypes they ends with []
        Set<String> types = new HashSet<String>();

        PhpType phpType = new PhpType();
        phpType.add(globalVars.get(variableName));

        for(String arrayType: PhpIndex.getInstance(psiElement.getProject()).completeType(psiElement.getProject(), phpType, new HashSet<String>()).getTypes()) {
            if(arrayType.endsWith("[]")) {
                types.add(arrayType.substring(0, arrayType.length() -2));
            }
        }

        globalVars.put(scopeVariable, types);

    }

    private static Collection<? extends PhpNamedElement> getRootVariableByName(PsiElement psiElement, String variableName) {

        ArrayList<PhpNamedElement> phpNamedElements = new ArrayList<PhpNamedElement>();
        for(Map.Entry<String, Set<String>> variable : collectScopeVariables(psiElement).entrySet()) {
            if(variable.getKey().equals(variableName)) {
                phpNamedElements.addAll(PhpElementsUtil.getClassFromPhpTypeSet(psiElement.getProject(), variable.getValue()));
            }

        }

        return phpNamedElements;

    }

    private static Collection<? extends PhpNamedElement> resolveTwigMethodName(Collection<? extends PhpNamedElement> previousElement, String typeName) {

        ArrayList<PhpNamedElement> phpNamedElements = new ArrayList<PhpNamedElement>();

        for(PhpNamedElement phpNamedElement: previousElement) {
            for(PhpNamedElement target : getTwigPhpNameTargets(phpNamedElement, typeName)) {
                PhpType phpType = target.getType();
                for(String typeString: phpType.getTypes()) {
                    PhpNamedElement phpNamedElement1 = PhpElementsUtil.getClassInterface(phpNamedElement.getProject(), typeString);
                    if(phpNamedElement1 != null) {
                        phpNamedElements.add(phpNamedElement1);
                    }
                }
            }
        }

        return phpNamedElements;
    }

    public static Collection<? extends PhpNamedElement> getTwigPhpNameTargets(PhpNamedElement phpNamedElement, String variableName) {

        // make it easy for use
        variableName = variableName.toLowerCase();

        ArrayList<PhpNamedElement> targets = new ArrayList<PhpNamedElement>();
        if(phpNamedElement instanceof PhpClass) {

            for(Method method: ((PhpClass) phpNamedElement).getMethods()) {
                String methodName = method.getName().toLowerCase();
                if(method.getModifier().isPublic() && (methodName.equals(variableName) || methodName.equals("get" + variableName))) {
                    targets.add(method);
                }
            }

            for(Field field: ((PhpClass) phpNamedElement).getFields()) {
                String fieldName = field.getName().toLowerCase();
                if(field.getModifier().isPublic() && fieldName.equals(variableName)) {
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

        if(types.size() > 0) {
            return types.iterator().next();
        }

        return "";

    }

}

