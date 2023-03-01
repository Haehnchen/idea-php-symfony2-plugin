package fr.adrienbrault.idea.symfony2plugin.assistant.signature;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeProvider implements PhpTypeProvider4 {

    final static char TRIM_KEY = '\u0181';
    private static final ExtensionPointName<MethodSignatureTypeProviderExtension> EXTENSIONS = new ExtensionPointName<>("fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderExtension");

    @Override
    public char getKey() {
        return '\u0160';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (!Settings.getInstance(e.getProject()).pluginEnabled || !(e instanceof MethodReference methodReference)) {
            return null;
        }

        Collection<MethodSignatureSetting> signatures = getSignatureSettings(e);
        if(signatures.size() == 0) {
            return null;
        }

        Collection<MethodSignatureSetting> matchedSignatures = getSignatureSetting(methodReference.getName(), signatures);
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
                        return new PhpType().add("#" + this.getKey() + refSignature + TRIM_KEY + param);
                    }
                }

                String parameterSignature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(methodReference, TRIM_KEY);
                if(parameterSignature != null) {
                    return new PhpType().add("#" + this.getKey() + parameterSignature);
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public PhpType complete(String s, Project project) {
        return null;
    }

    @NotNull
    private Collection<MethodSignatureSetting> getSignatureSettings(@NotNull PsiElement psiElement) {
        Collection<MethodSignatureSetting> signatures = new ArrayList<>();

        // get user defined settings
        if(Settings.getInstance(psiElement.getProject()).objectSignatureTypeProvider) {
            Collection<MethodSignatureSetting> settingSignatures = Settings.getInstance(psiElement.getProject()).methodSignatureSettings;
            if(settingSignatures != null) {
                signatures.addAll(settingSignatures);
            }
        }

        // load extension
        MethodSignatureTypeProviderExtension[] extensions = EXTENSIONS.getExtensions();
        if(extensions.length > 0) {
            MethodSignatureTypeProviderParameter parameter = new MethodSignatureTypeProviderParameter(psiElement);
            for(MethodSignatureTypeProviderExtension extension: extensions){
                signatures.addAll(extension.getSignatures(parameter));
            }
        }

        return signatures;
    }

    private Collection<MethodSignatureSetting> getSignatureSetting(String methodName, Collection<MethodSignatureSetting> methodSignatureSettingList) {

        Collection<MethodSignatureSetting> matchedSignatures = new ArrayList<>();

        for(MethodSignatureSetting methodSignatureSetting: methodSignatureSettingList) {
            if(methodSignatureSetting.getMethodName().equals(methodName)) {
                matchedSignatures.add(methodSignatureSetting);
            }
        }

        return matchedSignatures;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
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

        Collection<MethodSignatureSetting> signatures = getSignatureSettings(phpNamedElement);
        if(signatures.size() == 0) {
            return null;
        }

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if(parameter == null) {
            return null;
        }

        Collection<PhpNamedElement> phpNamedElements = new ArrayList<>();

        for(MethodSignatureSetting matchedSignature: signatures) {
            for(PhpTypeSignatureInterface signatureTypeProvider: PhpTypeSignatureTypes.DEFAULT_PROVIDER) {
                if( signatureTypeProvider.getName().equals(matchedSignature.getReferenceProviderName()) && PhpElementsUtil.isMethodInstanceOf((Method) phpNamedElement, matchedSignature.getCallTo(), matchedSignature.getMethodName())) {
                    Collection<? extends PhpNamedElement> namedElements = signatureTypeProvider.getByParameter(project, parameter);
                    if(namedElements != null) {
                        phpNamedElements.addAll(namedElements);
                    }
                }
            }
        }

        // not good but we need return any previous types: null clears all types
        return new ArrayList<>(phpNamedElements);
    }
}
