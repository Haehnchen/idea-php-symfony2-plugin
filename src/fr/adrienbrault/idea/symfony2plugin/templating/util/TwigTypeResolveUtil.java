package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.util.Condition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigTypeResolveUtil {

    public static String[] formatPsiTypeName(PsiElement psiElement) {

        String typeNames = PhpElementsUtil.getPrevSiblingAsTextUntil(psiElement, PlatformPatterns.psiElement(PsiWhiteSpace.class)).trim();
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
            return null;
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
                Collections.emptyList();
            }

        }

        return type;
    }

    private static Collection<? extends PhpNamedElement> findInlineDocBlockVariableByName(PsiElement psiInsideBlock, String variableName) {

        PsiElement twigCompositeElement = PsiTreeUtil.findFirstParent(psiInsideBlock, new Condition<PsiElement>() {
            @Override
            public boolean value(PsiElement psiElement) {
                if (psiElement instanceof TwigCompositeElement) {
                    if (PlatformPatterns.psiElement(TwigElementTypes.BLOCK_STATEMENT).accepts(psiElement)) {
                        return true;
                    }
                }
                return false;
            }
        });


        Pattern pattern = Pattern.compile("\\{#[\\s]+" + Pattern.quote(variableName) + "[\\s]+(.*)[\\s]+#}");
        Collection<PsiComment> psiComments = PsiTreeUtil.findChildrenOfType(twigCompositeElement, PsiComment.class);

        ArrayList<PhpNamedElement> arrayList = new ArrayList<PhpNamedElement>();
        for(PsiComment psiComment: psiComments) {
            Matcher matcher = pattern.matcher(psiComment.getText());
            if (matcher.find()) {

                // think of multi resolve
                PhpClass phpClass = PhpElementsUtil.getClass(psiComment.getProject(), matcher.group(1));
                if(phpClass != null) {
                    arrayList.add(phpClass);
                    return arrayList;
                }

            }
        }

        return null;
    }

    private static Collection<? extends PhpNamedElement> getRootVariableByName(PsiElement psiElement, String variableName) {

        HashMap<String, String> globalVars = new HashMap<String, String>();
        globalVars.put("app", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\GlobalVariables");

        ArrayList<PhpNamedElement> phpNamedElements = new ArrayList<PhpNamedElement>();

        // parameter prio?
        if(globalVars.containsKey(variableName)) {
            PhpClass phpClass = PhpElementsUtil.getClass(psiElement.getProject(), globalVars.get(variableName));
            if(phpClass != null) {
                phpNamedElements.add(phpClass);
                return phpNamedElements;
            }
        }


        return findInlineDocBlockVariableByName(psiElement, variableName);
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
                if(methodName.equals(variableName) || methodName.equals("get" + variableName)) {
                    targets.add(method);
                }
            }
        }

        return targets;
    }

}
