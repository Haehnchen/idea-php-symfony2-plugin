package fr.adrienbrault.idea.symfony2plugin.assistant.signature;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeProvider implements PhpTypeProvider2 {

    final static char TRIM_KEY = '\u0181';
    private static final ExtensionPointName<MethodSignatureTypeProviderExtension> EXTENSIONS = new ExtensionPointName<MethodSignatureTypeProviderExtension>("fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderExtension");

    @Override
    public char getKey() {
        return '\u0160';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {

        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled || !(e instanceof MethodReference)) {
            return null;
        }

        List<MethodSignatureSetting> signatures = new ArrayList<MethodSignatureSetting>();

        // get user defined settings
        if(Settings.getInstance(e.getProject()).objectSignatureTypeProvider) {
            List<MethodSignatureSetting> settingSignatures = Settings.getInstance(e.getProject()).methodSignatureSettings;
            if(settingSignatures != null) {
                signatures.addAll(settingSignatures);
            }
        }

        // load extension
        MethodSignatureTypeProviderExtension[] extensions = EXTENSIONS.getExtensions();
        if(extensions.length > 0) {
            MethodSignatureTypeProviderParameter parameter = new MethodSignatureTypeProviderParameter(e);
            for(MethodSignatureTypeProviderExtension extension: extensions){
                signatures.addAll(extension.getSignatures(parameter));
            }
        }

        // we not have custom settings
        if(signatures.size() == 0) {
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

                // @TODO: use PhpTypeProviderUtil
                if (parameter instanceof PhpReference && (parameter instanceof ClassConstantReference || parameter instanceof FieldReference)) {
                    String signature = ((PhpReference) parameter).getSignature();
                    if (StringUtil.isNotEmpty(signature)) {
                        return refSignature + TRIM_KEY + signature;
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
        int endIndex = expression.lastIndexOf(TRIM_KEY);
        if(endIndex == -1) {
            return null;
        }

        String originalSignature = expression.substring(0, endIndex);
        String parameter = expression.substring(endIndex + 1);

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);
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
        phpNamedElements.add(phpNamedElement);

        PhpTypeSignatureInterface[] signatureTypeProviders = PhpTypeSignatureTypes.DEFAULT_PROVIDER;

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if(parameter == null) {
            return phpNamedElementCollections;
        }

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

        // not good but we need return any previous types: null clears all types
        return new ArrayList<PhpNamedElement>(phpNamedElements);
    }

}
