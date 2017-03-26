package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlGoToKnownDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        List<PsiElement> results = new ArrayList<>();

        if(YamlElementPatternHelper.getSingleLineScalarKey("_controller").accepts(psiElement)) {
            this.getControllerGoto(psiElement, results);
        }

        if(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(psiElement)) {
            this.getClassGoto(psiElement, results);
        }

        if(YamlElementPatternHelper.getSingleLineScalarKey("resource").accepts(psiElement)) {
            this.attachResourceBundleGoto(psiElement, results);
            this.attachResourceOnPathGoto(psiElement, results);
        }

        // tags: { name: foobar }
        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("name")
        ).accepts(psiElement)) {
            this.getTagClassesGoto(psiElement, results);
        }

        // tags: [ name: foobar ]
        if(YamlElementPatternHelper.getTagsAsSequencePattern().accepts(psiElement)) {
            this.getTagClassesGoto(psiElement, results);
        }

        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("event")
        ).accepts(psiElement)) {
            this.getEventGoto(psiElement, results);
        }

        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("calls")
        ).accepts(psiElement)) {
            this.getMethodGoto(psiElement, results);
        }

        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("method")
        ).accepts(psiElement)) {
            this.getTagMethodGoto(psiElement, results);
        }

        // ["@service", method]
        if(YamlElementPatternHelper.getAfterCommaPattern().accepts(psiElement)) {
            this.getArrayMethodGoto(psiElement, results);
        }

        // config.yml: "as<caret>setic": ~
        if(PlatformPatterns.psiElement().inFile(YamlElementPatternHelper.getConfigFileNamePattern()).accepts(psiElement)) {
            this.visitConfigKey(psiElement, results);
        }

        // factory: "service:method"
        if(YamlElementPatternHelper.getSingleLineScalarKey("factory").accepts(psiElement)) {
            this.getFactoryStringGoto(psiElement, results);
        }

        // services:
        //   My<caret>Class: ~
        if(YamlElementPatternHelper.getServicesKeyPattern().accepts(psiElement)) {
            getClassesForServiceKey(psiElement, results);
        }

        return results.toArray(new PsiElement[results.size()]);
    }

    private void visitConfigKey(@NotNull PsiElement psiElement, @NotNull Collection<PsiElement> results) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof YAMLKeyValue)) {
            return;
        }

        String keyText = ((YAMLKeyValue) parent).getKeyText();
        if(StringUtils.isBlank(keyText)) {
            return;
        }

        results.addAll(ConfigUtil.getTreeSignatureTargets(psiElement.getProject(), keyText));
    }

    private void getArrayMethodGoto(PsiElement psiElement, List<PsiElement> results) {

        String text = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(text)) {
            return;
        }

        String service = YamlHelper.getPreviousSequenceItemAsText(psiElement);
        if (service == null) {
            return;
        }

        PhpClass phpClass = ServiceUtil.getServiceClass(psiElement.getProject(), service);
        if(phpClass == null) {
            return;
        }

        for (Method method : phpClass.getMethods()) {
            if(text.equals(method.getName())) {
                results.add(method);
            }
        }
    }

    /**
     * Factory goto: "factory: 'foo:bar'"
     */
    private void getFactoryStringGoto(PsiElement psiElement, List<PsiElement> results) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof YAMLScalar)) {
            return;
        }

        String textValue = ((YAMLScalar) parent).getTextValue();
        String[] split = textValue.split(":");
        if(split.length != 2) {
            return;
        }

        PhpClass phpClass = ServiceUtil.getServiceClass(psiElement.getProject(), split[0]);
        if(phpClass == null) {
            return;
        }

        for (Method method : phpClass.getMethods()) {
            if(split[1].equals(method.getName())) {
                results.add(method);
            }
        }
    }

    private void getClassGoto(PsiElement psiElement, List<PsiElement> results) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), text);
        if(phpClass != null) {
            results.add(phpClass);
        }

    }

    private void getMethodGoto(PsiElement psiElement, List<PsiElement> results) {

        YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getParentOfType(psiElement, YAMLCompoundValue.class);
        if(yamlCompoundValue == null) {
            return;
        }

        yamlCompoundValue = PsiTreeUtil.getParentOfType(yamlCompoundValue, YAMLCompoundValue.class);
        if(yamlCompoundValue == null) {
            return;
        }

        YAMLKeyValue classKeyValue = PsiElementUtils.getChildrenOfType(yamlCompoundValue, PlatformPatterns.psiElement(YAMLKeyValue.class).withName("class"));
        if(classKeyValue == null) {
            return;
        }

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classKeyValue.getValueText());
        if(phpClass != null) {
            for(Method method: PhpElementsUtil.getClassPublicMethod(phpClass)) {
                if(method.getName().equals(PsiElementUtils.trimQuote(psiElement.getText()))) {
                    results.add(method);
                }
            }
        }

    }

    private void getTagMethodGoto(PsiElement psiElement, List<PsiElement> results) {

        String methodName = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(methodName)) {
            return;
        }

        String classValue = YamlHelper.getServiceDefinitionClass(psiElement);
        if(classValue == null) {
            return;
        }


        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classValue);
        if(phpClass == null) {
            return;
        }

        Method method = phpClass.findMethodByName(methodName);
        if(method != null) {
            results.add(method);
        }

    }

    private void attachResourceBundleGoto(PsiElement psiElement, List<PsiElement> results) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(text)) {
            return;
        }

        results.addAll(FileResourceUtil.getFileResourceTargetsInBundleScope(psiElement.getProject(), text));
        results.addAll(FileResourceUtil.getFileResourceTargetsInBundleDirectory(psiElement.getProject(), text));
    }

    private void attachResourceOnPathGoto(PsiElement psiElement, List<PsiElement> results) {

        String text = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(text) || text.startsWith("@")) {
            return;
        }

        PsiFile containingFile = psiElement.getContainingFile();
        if(containingFile == null) {
            return;
        }

        results.addAll(FileResourceUtil.getFileResourceTargetsInDirectoryScope(containingFile, text));
    }

    private void getControllerGoto(PsiElement psiElement, List<PsiElement> results) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isNotBlank(text)) {
            results.addAll(Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), text)));
        }
    }

    private void getTagClassesGoto(PsiElement psiElement, List<PsiElement> results) {
        String tagName = PsiElementUtils.trimQuote(psiElement.getText());

        if(StringUtils.isNotBlank(tagName)) {
            results.addAll(ServiceUtil.getTaggedClassesWithCompiled(psiElement.getProject(), tagName));
        }
    }

    private void getEventGoto(PsiElement psiElement, List<PsiElement> results) {
        results.addAll(EventDispatcherSubscriberUtil.getEventPsiElements(psiElement.getProject(), PsiElementUtils.trimQuote(psiElement.getText())));
    }

    /**
     * services:
     *   My<caret>Class: ~
     */
    private void getClassesForServiceKey(PsiElement psiElement, List<PsiElement> results) {
        PsiElement parent = psiElement.getParent();
        if(parent instanceof YAMLKeyValue) {
            String valueText = ((YAMLKeyValue) parent).getKeyText();
            if(StringUtils.isNotBlank(valueText)) {
                results.addAll(PhpElementsUtil.getClassesInterface(psiElement.getProject(), valueText));
            }
        }
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
