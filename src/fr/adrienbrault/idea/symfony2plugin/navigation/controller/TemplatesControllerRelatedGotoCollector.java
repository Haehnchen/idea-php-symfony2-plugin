package fr.adrienbrault.idea.symfony2plugin.navigation.controller;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.TwigIcons;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplatesControllerRelatedGotoCollector implements ControllerActionGotoRelatedCollector {

    @Override
    public void collectGotoRelatedItems(ControllerActionGotoRelatedCollectorParameter parameter) {

        Set<String> uniqueTemplates = new HashSet<>();

        // on @Template annotation
        PhpDocComment phpDocComment = parameter.getMethod().getDocComment();
        if(phpDocComment != null) {
            Collection<PhpDocTag> phpDocTags = AnnotationBackportUtil.filterValidDocTags(PsiTreeUtil.findChildrenOfType(phpDocComment, PhpDocTag.class));
            if(phpDocTags.size() > 0) {
                // cache use map for this phpDocComment
                Map<String, String> importMap = AnnotationUtil.getUseImportMap(phpDocComment);
                if(importMap.size() > 0) {
                    for(PhpDocTag phpDocTag: phpDocTags) {

                        // resolve annotation and check for template
                        PhpClass phpClass = AnnotationUtil.getAnnotationReference(phpDocTag, importMap);
                        if(phpClass != null && PhpElementsUtil.isEqualClassName(phpClass, TwigHelper.TEMPLATE_ANNOTATION_CLASS)) {
                            for(Map.Entry<String, PsiElement> entry: TwigUtil.getTemplateAnnotationFiles(phpDocTag).entrySet()) {
                                if(!uniqueTemplates.contains(entry.getKey())) {
                                    parameter.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(entry.getValue(), TwigUtil.getFoldingTemplateNameOrCurrent(entry.getKey())).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                                    uniqueTemplates.add(entry.getKey());
                                }
                            }
                        }
                    }
                }

            }

        }

        // on method name
        String[] templateNames = TwigUtil.getControllerMethodShortcut(parameter.getMethod());
        if(templateNames != null) {
            for (String templateName : templateNames) {
                for(PsiElement templateTarget: TwigHelper.getTemplatePsiElements(parameter.getProject(), templateName)) {
                    if(!uniqueTemplates.contains(templateName)) {
                        parameter.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, TwigUtil.getFoldingTemplateNameOrCurrent(templateName)).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                        uniqueTemplates.add(templateName);
                    }
                }
            }

        }

        for(PsiElement psiElement: parameter.getParameterLists()) {
            MethodMatcher.MethodMatchParameter matchedSignature = MethodMatcher.getMatchedSignatureWithDepth(psiElement, SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES);
            if (matchedSignature != null) {
                String resolveString = PhpElementsUtil.getStringValue(psiElement);
                if(resolveString != null && !uniqueTemplates.contains(resolveString)) {
                    uniqueTemplates.add(resolveString);
                    for(PsiElement templateTarget: TwigHelper.getTemplatePsiElements(parameter.getProject(), resolveString)) {
                        parameter.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, resolveString).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                    }
                }
            }

        }

    }

}
