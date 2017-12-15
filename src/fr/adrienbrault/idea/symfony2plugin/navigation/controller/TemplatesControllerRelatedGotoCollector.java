package fr.adrienbrault.idea.symfony2plugin.navigation.controller;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplatesControllerRelatedGotoCollector implements ControllerActionGotoRelatedCollector {

    @Override
    public void collectGotoRelatedItems(ControllerActionGotoRelatedCollectorParameter parameter) {
        Set<String> uniqueTemplates = new HashSet<>();

        Method method = parameter.getMethod();

        visitMethodTemplateNames(method, pair -> {
            String templateName = pair.getFirst();

            if(!uniqueTemplates.contains(templateName)) {
                uniqueTemplates.add(templateName);

                for (PsiElement psiElement : pair.getSecond()) {
                    parameter.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(
                        psiElement,
                        TwigUtil.getFoldingTemplateNameOrCurrent(templateName)).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER)
                    );
                }
            }
        });

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

    /**
     *  Visit every possible template on given method: eg Annotations @Template()
     */
    public static void visitMethodTemplateNames(@NotNull Method method, @NotNull Consumer<Pair<String, PsiElement[]>> consumer) {
        // on @Template annotation
        PhpDocComment phpDocComment = method.getDocComment();
        if(phpDocComment != null) {
            Collection<PhpDocTag> phpDocTags = AnnotationBackportUtil.filterValidDocTags(PsiTreeUtil.findChildrenOfType(phpDocComment, PhpDocTag.class));
            if(phpDocTags.size() > 0) {
                // cache use map for this phpDocComment
                Map<String, String> importMap = AnnotationBackportUtil.getUseImportMap(phpDocComment);
                if(importMap.size() > 0) {
                    for(PhpDocTag phpDocTag: phpDocTags) {
                        // resolve annotation and check for template
                        PhpClass phpClass = AnnotationBackportUtil.getAnnotationReference(phpDocTag, importMap);
                        if(phpClass != null && PhpElementsUtil.isEqualClassName(phpClass, TwigHelper.TEMPLATE_ANNOTATION_CLASS)) {
                            Pair<String, PsiElement[]> templateAnnotationFiles = TwigUtil.getTemplateAnnotationFiles(phpDocTag);
                            if(templateAnnotationFiles != null) {
                                consumer.accept(Pair.create(templateAnnotationFiles.getFirst(), templateAnnotationFiles.getSecond()));
                            }
                        }
                    }
                }
            }
        }

        // on method name
        for (String templateName : TwigUtil.getControllerMethodShortcut(method)) {
            consumer.accept(Pair.create(templateName, TwigHelper.getTemplatePsiElements(method.getProject(), templateName)));
        }
    }
}
