package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyContainerTypeProvider implements PhpTypeProvider2 {

    final static char TRIM_KEY = '\u0180';

    @Override
    public char getKey() {
        return '\u0150';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {

        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled || !Settings.getInstance(e.getProject()).symfonyContainerTypeProvider) {
            return null;
        }

        // container calls are only on "get" methods
        if(!(e instanceof MethodReference) || !PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "get")) {
            return null;
        }

        String refSignature = ((MethodReference)e).getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }

        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        PsiElement[] parameters = ((MethodReference)e).getParameters();
        if (parameters.length == 1) {
            PsiElement parameter = parameters[0];
            if (parameter instanceof StringLiteralExpression) {
                String param = ((StringLiteralExpression)parameter).getContents();
                if (StringUtil.isNotEmpty(param)) {
                    return refSignature + TRIM_KEY + param;
                }
            }

            // whitelist here; we can also provide some more but think of performance
            // Service::NAME and $this->name;
            if (parameter instanceof PhpReference && (parameter instanceof ClassConstantReference || parameter instanceof FieldReference)) {
                String signature = ((PhpReference) parameter).getSignature();
                if (StringUtil.isNotEmpty(signature)) {
                    return refSignature + TRIM_KEY + signature;
                }
            }

        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        // get back our original call
        String originalSignature = expression.substring(0, expression.lastIndexOf(TRIM_KEY));
        String parameter = expression.substring(expression.lastIndexOf(TRIM_KEY) + 1);

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature(originalSignature, null, 0);
        if(phpNamedElementCollections.size() == 0) {
            return Collections.emptySet();
        }

        // get first matched item
        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Method)) {
            return Collections.emptySet();
        }

        // we can also pipe php references signatures and resolve them here
        // overwrite parameter to get string value
        if(parameter.startsWith("#")) {
            Collection<? extends PhpNamedElement> signTypes = phpIndex.getBySignature(parameter, null, 0);
            if(signTypes.size() == 0) {
                return Arrays.asList(phpNamedElement);
            }

            parameter = PhpElementsUtil.getStringValue(signTypes.iterator().next());
            if(parameter == null) {
                return Arrays.asList(phpNamedElement);
            }
        }

        // finally search the classes
        if(new Symfony2InterfacesUtil().isContainerGetCall((Method) phpNamedElement)) {
            ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap();
            String serviceClass = serviceMap.getMap().get(parameter);
            if(serviceClass != null) {
                return PhpIndex.getInstance(project).getAnyByFQN(serviceClass);
            }
        }

        return Arrays.asList(phpNamedElement);
    }

}
