package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import kotlin.Pair;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HtmlTemplateGoToDeclarationHandler implements GotoDeclarationHandler {
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int offset, Editor editor) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        Collection<PsiElement> targets = new ArrayList<>();

        // <a href="">
        boolean isUrl = psiElement instanceof XmlToken
            && psiElement.getNode().getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
            && (TwigHtmlCompletionUtil.getHrefAttributePattern().accepts(psiElement) || TwigHtmlCompletionUtil.getFormActionAttributePattern().accepts(psiElement));

        if (isUrl && psiElement.getParent() instanceof XmlAttributeValue xmlAttributeValue) {
            String url = xmlAttributeValue.getValue();
            if (!url.isBlank()) {
                Project project = psiElement.getProject();
                for (Route route : RouteHelper.findRoutesByPath(project, url)) {
                    targets.addAll(RouteHelper.getRouteNameTarget(project, route.getName()));
                }
            }
        }

        // <twig:a
        if (psiElement instanceof XmlToken && psiElement.getNode().getElementType() == XmlTokenType.XML_NAME && psiElement.getText().startsWith("twig:")) {
            String text = psiElement.getText();
            if (!text.startsWith("twig:")) {
                return null;
            }


            int calulatedOffset = offset - psiElement.getTextRange().getStartOffset();
            if (calulatedOffset < 0) {
                calulatedOffset = 5;
            }

            Project project = psiElement.getProject();

            // <twig:a
            if (calulatedOffset > 5) {
                if (TwigHtmlCompletionUtil.getTwigNamespacePattern().accepts(psiElement)) {

                    String componentName = StringUtils.stripStart(text, "twig:");
                    if (!componentName.isBlank()) {
                        targets.addAll(UxUtil.getTwigComponentNameTargets(project, componentName));
                    }
                }
            } else {
                // <twig
                targets.addAll(UxUtil.getTwigComponentAllTargets(project));
            }
        }

        // <twig:Foo :message="" message="">
        if (psiElement instanceof XmlToken) {
            PsiElement parent = psiElement.getParent();
            if (parent.getNode().getElementType() == XmlElementType.XML_ATTRIBUTE) {
                if (parent.getParent() instanceof HtmlTag htmlTag && htmlTag.getName().startsWith("twig:")) {
                    String text = psiElement.getText();
                    Project project = psiElement.getProject();

                    for (PhpClass phpClass : UxUtil.getTwigComponentNameTargets(project, htmlTag.getName().substring(5))) {
                        UxUtil.visitComponentVariables(phpClass, pair -> {
                            if (pair.getFirst().equals(StringUtils.stripStart(text, ":"))) {
                                targets.add(pair.getSecond());
                            }
                        });
                    }
                };
            }
        }

        return targets.toArray(new PsiElement[0]);
    }
}
