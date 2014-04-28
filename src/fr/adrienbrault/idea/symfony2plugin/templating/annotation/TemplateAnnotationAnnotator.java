package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.psi.util.PsiTreeUtil;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String templateName;

        // @Template("FooBundle:Folder:foo.html.twig")
        // @Template("FooBundle:Folder:foo.html.twig", "asdas")
        // @Template(tag="name")
        Matcher matcher = Pattern.compile("\\(\"(.*)\"").matcher(tagValue);
        if (matcher.find()) {
            templateName = matcher.group(1);
        } else {

            // find template name on last method
            PhpDocComment docComment = PsiTreeUtil.getParentOfType(parameter.getPhpDocTag(), PhpDocComment.class);
            if(null == docComment) {
                return;
            }

            Method method = PsiTreeUtil.getNextSiblingOfType(docComment, Method.class);
            if(null == method || !method.getName().endsWith("Action")) {
                return;
            }

            templateName = TwigUtil.getControllerMethodShortcut(method);
        }

        if(templateName == null) {
            return;
        }

        if ( TwigHelper.getTemplatePsiElements(parameter.getProject(), templateName).length > 0) {
            return;
        }

        if(null != parameter.getPhpDocTag().getFirstChild()) {
            parameter.getHolder().createWarningAnnotation(parameter.getPhpDocTag().getFirstChild().getTextRange(), "Create Template")
                .registerFix(new PhpTemplateAnnotator.CreateTemplateFix(templateName));
        }


    }

}
