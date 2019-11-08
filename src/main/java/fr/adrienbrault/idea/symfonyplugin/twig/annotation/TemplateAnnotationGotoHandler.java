package fr.adrienbrault.idea.symfonyplugin.twig.annotation;

import com.intellij.psi.PsiElement;
import de.espend.idea.php.annotation.extension.PhpAnnotationDocTagGotoHandler;
import de.espend.idea.php.annotation.extension.parameter.AnnotationDocTagGotoHandlerParameter;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateAnnotationGotoHandler implements PhpAnnotationDocTagGotoHandler {
    @Override
    public void getGotoDeclarationTargets(AnnotationDocTagGotoHandlerParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return;
        }

        if(!PhpElementsUtil.isEqualClassName(parameter.getPhpClass(), TwigUtil.TEMPLATE_ANNOTATION_CLASS)) {
            return;
        }

        for (Collection<PsiElement> psiElements : TwigUtil.getTemplateAnnotationFilesWithSiblingMethod(parameter.getPhpDocTag()).values()) {
            parameter.addTargets(psiElements);
        }
    }
}
