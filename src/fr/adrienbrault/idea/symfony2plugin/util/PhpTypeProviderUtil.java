package fr.adrienbrault.idea.symfony2plugin.util;


import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class PhpTypeProviderUtil {

    @Nullable
    public static String getReferenceSignature(MethodReference methodReference, char trimKey) {
        return getReferenceSignature(methodReference, trimKey, 1);
    }

    @Nullable
    public static String getReferenceSignature(MethodReference methodReference, char trimKey, int equalParameterCount) {

        String refSignature = methodReference.getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }

        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length != equalParameterCount) {
            return null;
        }

        PsiElement parameter = parameters[0];

        // we already have a string value
        if ((parameter instanceof StringLiteralExpression)) {
            String param = ((StringLiteralExpression)parameter).getContents();
            if (StringUtil.isNotEmpty(param)) {
                return refSignature + trimKey + param;
            }

            return null;
        }

        // whitelist here; we can also provide some more but think of performance
        // Service::NAME, $this->name and Entity::CLASS;
        if (parameter instanceof PhpReference && (parameter instanceof ClassConstantReference || parameter instanceof FieldReference)) {
            String signature = ((PhpReference) parameter).getSignature();
            if (StringUtil.isNotEmpty(signature)) {
                return refSignature + trimKey + signature;
            }

            return null;
        }

        return null;
    }

    /**
     * we can also pipe php references signatures and resolve them here
     * overwrite parameter to get string value
     */
    @Nullable
    public static String getResolvedParameter(@NotNull PhpIndex phpIndex, @NotNull String parameter) {

        // PHP 5.5 class constant: "Class\Foo::class"
        if(parameter.startsWith("#K#C")) {
            // PhpStorm9: #K#C\Class\Foo.class
            if(parameter.endsWith(".class")) {
                return parameter.substring(4, parameter.length() - 6);
            }

            // PhpStorm8: #K#C\Class\Foo.
            // workaround since signature has empty type
            if(parameter.endsWith(".")) {
                return parameter.substring(4, parameter.length() - 1);
            }
        }

        // #K#C\Class\Foo.property
        // #K#C\Class\Foo.CONST
        if(parameter.startsWith("#")) {

            // get psi element from signature
            Collection<? extends PhpNamedElement> signTypes = phpIndex.getBySignature(parameter, null, 0);
            if(signTypes.size() == 0) {
                return null;
            }

            // get string value
            parameter = PhpElementsUtil.getStringValue(signTypes.iterator().next());
            if(parameter == null) {
                return null;
            }

        }

        return parameter;
    }

}
