package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        // only string values like "foo", foo
        if (!PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement)
            && !PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement)
            && !PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING).withLanguage(YAMLLanguage.INSTANCE).accepts(psiElement)) {

            return new PsiElement[]{};
        }

        String psiText = PsiElementUtils.getText(psiElement);
        if(null == psiText || psiText.length() == 0) {
            return new PsiElement[]{};
        }

        List<PsiElement> psiElements = new ArrayList<PsiElement>();

        if(psiText.startsWith("@") && psiText.length() > 1) {
            psiElements.addAll(Arrays.asList((serviceGoToDeclaration(psiElement, psiText.substring(1)))));
        }

        // match: %annotations.reader.class%
        if(psiText.length() > 3 && psiText.startsWith("%") && psiText.endsWith("%")) {
            psiElements.addAll(Arrays.asList((parameterGoToDeclaration(psiElement, psiText))));
        }

        if(psiText.startsWith("!php/const:")) {
            psiElements.addAll(constantGoto(psiElement, psiText));
        }

        if(psiText.contains("\\")) {
            psiElements.addAll(classGoToDeclaration(psiElement, psiText)) ;
        }

        if(psiText.endsWith(".twig") || psiText.endsWith(".php")) {
            psiElements.addAll(templateGoto(psiElement, psiText));
        }

        if(psiText.matches("^[\\w_.]+") && getGlobalServiceStringPattern().accepts(psiElement)) {
            psiElements.addAll(Arrays.asList((serviceGoToDeclaration(psiElement, psiText))));
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    private Collection<PsiElement> classGoToDeclaration(PsiElement psiElement, String className) {

        Collection<PsiElement> psiElements = new HashSet<PsiElement>();

        // Class::method
        // Class::FooAction
        // Class:Foo
        if(className.contains(":")) {
            String[] split = className.replaceAll("(:)\\1", "$1").split(":");
            if(split.length == 2) {
                for(String append: new String[] {"", "Action"}) {
                    Method classMethod = PhpElementsUtil.getClassMethod(psiElement.getProject(), split[0], split[1] + append);
                    if(classMethod != null) {
                        psiElements.add(classMethod);
                    }
                }
            }

            return psiElements;
        }

        // ClassName
        psiElements.addAll(PhpElementsUtil.getClassesInterface(psiElement.getProject(), className));
        return psiElements;
    }

    private PsiElement[] serviceGoToDeclaration(PsiElement psiElement, String serviceId) {

        serviceId = YamlHelper.trimSpecialSyntaxServiceName(serviceId).toLowerCase();

        String serviceClass = ContainerCollectionResolver.resolveService(psiElement.getProject(), serviceId);

        if (serviceClass != null) {
            PsiElement[] targetElements = PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), serviceClass);
            if(targetElements.length > 0) {
                return targetElements;
            }
        }

        // get container target on indexes
        List<PsiElement> possibleServiceTargets = ServiceIndexUtil.findServiceDefinitions(psiElement.getProject(), serviceId);
        return possibleServiceTargets.toArray(new PsiElement[possibleServiceTargets.size()]);

    }

    private PsiElement[] parameterGoToDeclaration(PsiElement psiElement, String psiParameterName) {

        if(!YamlHelper.isValidParameterName(psiParameterName)) {
            return new PsiElement[0];
        }

        Collection<PsiElement> targets = ServiceUtil.getServiceClassTargets(psiElement.getProject(), psiParameterName);
        return targets.toArray(new PsiElement[targets.size()]);
    }

    private List<PsiFile> templateGoto(PsiElement psiElement, String templateName) {
        return Arrays.asList(TwigHelper.getTemplatePsiElements(psiElement.getProject(), templateName));
    }

    private Collection<PsiElement> constantGoto(@NotNull PsiElement psiElement, @NotNull String tagName) {
        String constantName = tagName.substring(11);

        if(!constantName.contains(":")) {
            return new ArrayList<>(PhpIndex.getInstance(psiElement.getProject()).getConstantsByFQN(constantName));
        }

        constantName = constantName.replaceAll(":+", ":");
        String[] split = constantName.split(":");

        Collection<PsiElement> psiElements = new ArrayList<>();
        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(psiElement.getProject(), split[0])) {
            Field fieldByName = phpClass.findFieldByName(split[1], true);
            if(fieldByName != null && fieldByName.isConstant()) {
                psiElements.add(fieldByName);
            }
        }

        return psiElements;
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

    /**
     * foo: b<caret>ar
     * foo: [ b<caret>ar ]
     * foo: { b<caret>ar }
     * foo:
     *  - b<caret>ar
     */
    private PsiElementPattern.Capture<PsiElement> getGlobalServiceStringPattern() {
        return PlatformPatterns.psiElement().withParent(
                PlatformPatterns.psiElement(YAMLScalar.class).withParent(PlatformPatterns.or(
                        PlatformPatterns.psiElement(YAMLKeyValue.class),
                        PlatformPatterns.psiElement(YAMLSequenceItem.class),
                        PlatformPatterns.psiElement(YAMLMapping.class)
                )));
    }
}
