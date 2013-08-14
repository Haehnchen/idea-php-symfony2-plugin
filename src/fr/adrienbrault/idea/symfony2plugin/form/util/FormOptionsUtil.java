package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormExtensionServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;

import java.util.*;

public class FormOptionsUtil {

    public static ArrayList<String> getExtendedTypeClasses(Project project, String... formTypeNames) {

        List<String> formTypeNamesList = Arrays.asList(formTypeNames);

        ArrayList<String> extendedTypeClasses = new ArrayList<String>();

        FormExtensionServiceParser formExtensionServiceParser = ServiceXmlParserFactory.getInstance(project, FormExtensionServiceParser.class);
        for(String formClass: formExtensionServiceParser.getFormExtensions().keySet()) {

            PsiElement psiElements[] = PhpElementsUtil.getPsiElementsBySignature(project, "#M#C\\" + formClass + ".getExtendedType");
            for(PsiElement psiElement: psiElements) {
                PhpReturn phpReturn = PsiTreeUtil.findChildOfType(psiElement, PhpReturn.class);
                if(phpReturn != null) {
                    PhpPsiElement returnValue = phpReturn.getFirstPsiChild();
                    if(returnValue instanceof StringLiteralExpression && formTypeNamesList.contains(((StringLiteralExpression) returnValue).getContents())) {
                        extendedTypeClasses.add(formClass);
                    }

                }
            }
        }

        return extendedTypeClasses;
    }

    public static HashMap<String, String> getFormExtensionKeys(Project project, String... formTypeNames) {
        HashMap<String, String> extensionKeys = new HashMap<String, String>();
        ArrayList<String> typeClasses = FormOptionsUtil.getExtendedTypeClasses(project, formTypeNames);

        for(String typeClass: typeClasses) {
            PsiElement psiElement = PhpElementsUtil.getPsiElementsBySignature(project,"#M#C\\" + typeClass + ".setDefaultOptions")[0];

            Collection<MethodReference> tests = PsiTreeUtil.findChildrenOfType(psiElement, MethodReference.class);
            for(MethodReference methodReference: tests) {
                // instance check
                // methodReference.getSignature().equals("#M#C\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface.setDefaults")
                if(methodReference.getName().equals("setDefaults")) {
                    PsiElement[] parameters = methodReference.getParameters();
                    if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                        for(String key: PhpElementsUtil.getArrayCreationKeys((ArrayCreationExpression) parameters[0])) {
                            extensionKeys.put(key, typeClass);
                        }
                    }

                }
            }
        }

        return extensionKeys;
    }

}
