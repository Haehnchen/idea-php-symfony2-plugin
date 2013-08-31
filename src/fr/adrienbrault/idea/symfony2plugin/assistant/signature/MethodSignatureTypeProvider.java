package fr.adrienbrault.idea.symfony2plugin.assistant.signature;

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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeProvider implements PhpTypeProvider2 {

    final static char TRIM_KEY = '\u0180';

    @Override
    public char getKey() {
        return '\u0160';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {

        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled) {
            return null;
        }

        List<MethodSignatureSetting> signatures = Settings.getInstance(e.getProject()).methodSignatureSettings;
        if(signatures == null || !(e instanceof MethodReference)) {
            return null;
        }

        MethodReference methodReference = (MethodReference) e;
        ArrayList<MethodSignatureSetting> matchedSignatures = getSignatureSetting(methodReference.getName(), signatures);
        if(matchedSignatures.size() == 0) {
            return null;
        }

        String refSignature = ((MethodReference)e).getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }

        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        PsiElement[] parameters = ((MethodReference)e).getParameters();
        for(MethodSignatureSetting methodSignature: matchedSignatures) {
            if (parameters.length - 1 >= methodSignature.getIndexParameter()) {
                PsiElement parameter = parameters[methodSignature.getIndexParameter()];
                if ((parameter instanceof StringLiteralExpression)) {
                    String param = ((StringLiteralExpression)parameter).getContents();
                    if (StringUtil.isNotEmpty(param)) {
                        return refSignature + TRIM_KEY + param;
                    }
                }
            }
        }

        return null;
    }

    private ArrayList<MethodSignatureSetting> getSignatureSetting(String methodName, List<MethodSignatureSetting> methodSignatureSettingList) {

        ArrayList<MethodSignatureSetting> matchedSignatures = new ArrayList<MethodSignatureSetting>();

        for(MethodSignatureSetting methodSignatureSetting: methodSignatureSettingList) {
            if(methodSignatureSetting.getMethodName().equals(methodName)) {
                matchedSignatures.add(methodSignatureSetting);
            }
        }

        return matchedSignatures;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        // get back our original call
        String originalSignature = expression.substring(0, expression.lastIndexOf(TRIM_KEY));
        String parameter = expression.substring(expression.lastIndexOf(TRIM_KEY) + 1);

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature(originalSignature, null, 0);
        if(phpNamedElementCollections.size() == 0) {
            return null;
        }

        // get first matched item
        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Method)) {
            return null;
        }

        List<MethodSignatureSetting> signatures = Settings.getInstance(project).methodSignatureSettings;
        if(signatures == null) {
            return null;
        }

        ArrayList<PhpNamedElement> phpNamedElements = new ArrayList<PhpNamedElement>();

        PhpTypeSignatureInterface[] signatureTypeProviders = PhpTypeSignatureTypes.DEFAULT_PROVIDER;

        for(MethodSignatureSetting matchedSignature: signatures) {
            for(PhpTypeSignatureInterface signatureTypeProvider: signatureTypeProviders) {
                if( signatureTypeProvider.getName().equals(matchedSignature.getReferenceProviderName()) && new Symfony2InterfacesUtil().isCallTo((Method) phpNamedElement, matchedSignature.getCallTo(), matchedSignature.getMethodName())) {
                    Collection<? extends PhpNamedElement> namedElements = signatureTypeProvider.getByParameter(project, parameter);
                    if(namedElements != null) {
                        phpNamedElements.addAll(namedElements);
                    }
                }
            }
        }

        if(phpNamedElements.size() > 0) {
            return new ArrayList<PhpNamedElement>(phpNamedElements);
        }

        return null;
    }

}
