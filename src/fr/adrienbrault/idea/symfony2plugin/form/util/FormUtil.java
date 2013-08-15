package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class FormUtil {

    @Nullable
    public static PhpClass getFormTypeToClass(Project project,@Nullable String formType) {

        if(formType == null) {
            return null;
        }

        // formtype can also be a direct class name
        if(formType.contains("\\")) {
            PhpClass phpClass = PhpElementsUtil.getClass(PhpIndex.getInstance(project), formType);
            if(phpClass != null) {
                return phpClass;
            }
        }

        // find on registered formtype aliases
        FormTypeServiceParser formTypeServiceParser = ServiceXmlParserFactory.getInstance(project, FormTypeServiceParser.class);
        String serviceName = formTypeServiceParser.getFormTypeMap().getServiceName(formType);
        if(serviceName == null) {
            return null;
        }

        String serviceClass = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().get(serviceName);
        if (null == serviceClass) {
            return null;
        }

        List<ResolveResult> resolveResults = PhpElementsUtil.getClassInterfaceResolveResult(project, serviceClass);
        if(resolveResults.size() == 0) {
            return null;
        }

        PsiElement psiElement = resolveResults.iterator().next().getElement();

        if(psiElement instanceof PhpClass) {
           return (PhpClass) resolveResults.iterator().next().getElement();
        }

        return null;

    }

}
