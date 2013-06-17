package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class SymfonyContainerTypeProvider implements PhpTypeProvider2 {

    @Override
    public char getKey() {
        return 'X';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {

        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled || !Settings.getInstance(e.getProject()).symfonyContainerTypeProvider) {
            return null;
        }

        // container calls are only on "get" methods
        if(!(e instanceof MethodReference) || !PhpElementsUtil.isMethodWithFirstString(e, "get")) {
            return null;
        }

        String refSignature = ((MethodReference)e).getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }

        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        PsiElement[] parameters = ((MethodReference)e).getParameters();
        if (parameters.length > 0) {
            PsiElement parameter = parameters[0];
            if ((parameter instanceof StringLiteralExpression)) {
                String param = ((StringLiteralExpression)parameter).getContents();
                if (StringUtil.isNotEmpty(param)) return "#X" + refSignature + "\\" + param;
            }
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        // i think we dont need this, but doesnt hurt
        if(!expression.startsWith("#X")) {
            return Collections.emptySet();
        }

        // get back our original call
        String orignalSignaturCall = expression.substring(2, expression.lastIndexOf("\\"));
        String parameter = expression.substring(expression.lastIndexOf("\\") + 1);

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature(orignalSignaturCall, null, 0);
        if(phpNamedElementCollections.size() == 0) {
            return Collections.emptySet();
        }

        // get first matched item
        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Method)) {
            return Collections.emptySet();
        }

        // finally search the classes
        if(new Symfony2InterfacesUtil().isContainerGetCall((Method) phpNamedElement)) {
            ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap();
            String serviceClass = serviceMap.getMap().get(parameter);
            if(serviceClass != null) {
                return PhpIndex.getInstance(project).getAnyByFQN(serviceClass);
            }
        }

        return Collections.emptySet();
    }

}
