package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import de.espend.idea.php.annotation.extension.PhpAnnotationDocTagAnnotator;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationDocTagAnnotatorParameter;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.PhpTemplateAnnotator;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateAnnotationAnnotator implements PhpAnnotationDocTagAnnotator {

    @Override
    public void annotate(PhpAnnotationDocTagAnnotatorParameter parameter) {

        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject()) ||
           !Settings.getInstance(parameter.getProject()).phpAnnotateTemplateAnnotation ||
           !PhpElementsUtil.isEqualClassName(parameter.getAnnotationClass(), TwigHelper.TEMPLATE_ANNOTATION_CLASS))
        {
            return;
        }

        PhpPsiElement phpDocAttrList = parameter.getPhpDocTag().getFirstPsiChild();
        if(phpDocAttrList == null) {
            return;
        }

        String tagValue = phpDocAttrList.getText();
        Collection<String> templateNames = new HashSet<>();

        // @Template("FooBundle:Folder:foo.html.twig")
        // @Template("FooBundle:Folder:foo.html.twig", "asdas")
        // @Template(tag="name")
        Matcher matcher = Pattern.compile("\\(\"(.*)\"").matcher(tagValue);
        if (matcher.find()) {
            templateNames.add(matcher.group(1));
        } else {

            // find template name on last method
            PhpDocComment docComment = PsiTreeUtil.getParentOfType(parameter.getPhpDocTag(), PhpDocComment.class);
            if(null == docComment) {
                return;
            }

            Method method = PsiTreeUtil.getNextSiblingOfType(docComment, Method.class);
            if(null == method || (!method.getName().endsWith("Action") && !method.getName().equalsIgnoreCase("__invoke"))) {
                return;
            }

            String[] controllerMethodShortcut = TwigUtil.getControllerMethodShortcut(method);
            if(controllerMethodShortcut != null) {
                templateNames.addAll(Arrays.asList(controllerMethodShortcut));
            }
        }

        if(templateNames.size() == 0) {
            return;
        }

        for (String templateName : templateNames) {
            if (TwigHelper.getTemplatePsiElements(parameter.getProject(), templateName).length > 0) {
                return;
            }
        }

        // find html target, as this this our first priority for end users condition
        String templateName = ContainerUtil.find(templateNames, s -> s.toLowerCase().endsWith(".html.twig"));

        // fallback on first item
        if(templateName == null) {
            templateName = templateNames.iterator().next();
        }

        // add fix to doc tag
        PsiElement firstChild = parameter.getPhpDocTag().getFirstChild();
        if(null == firstChild) {
            return;
        }

        parameter.getHolder()
            .createWarningAnnotation(firstChild.getTextRange(), "Create Template")
            .registerFix(new PhpTemplateAnnotator.CreateTemplateFix(templateName))
        ;
    }

}
