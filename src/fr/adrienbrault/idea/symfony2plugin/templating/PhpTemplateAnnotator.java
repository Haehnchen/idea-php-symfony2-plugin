package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


public class PhpTemplateAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!Settings.getInstance(element.getProject()).phpAnnotateTemplate) {
            return;
        }


        MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element);
        if (methodReference == null || !new Symfony2InterfacesUtil().isTemplatingRenderCall(methodReference)) {
            return;
        }

        ParameterBag parameterBag = PsiElementUtils.getCurrentParameterIndex(element.getParent());
        if(parameterBag == null || parameterBag.getIndex() != 0) {
            return;
        }

        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(element.getProject());
        String templateName = Symfony2InterfacesUtil.getFirstArgumentStringValue(methodReference);

        if(twigFilesByName.containsKey(templateName))  {
           return;
        }

        holder.createWarningAnnotation(element, "Missing Template");

    }
}