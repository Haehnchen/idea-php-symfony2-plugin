package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTypeProviderUtil {
    /**
     * Deprecated for external plugins
     */
    @Deprecated
    public static String getReferenceSignature(MethodReference methodReference, char trimKey) {
        return getReferenceSignatureByFirstParameter(methodReference, trimKey);
    }

    /**
     * Deprecated for external plugins
     */
    @Deprecated
    public static String getReferenceSignature(MethodReference methodReference, char trimKey, int equalParameterCount) {
        return getReferenceSignatureByFirstParameter(methodReference, trimKey);
    }

    /**
     * Creates a signature for PhpType implementation which must be resolved inside 'getBySignature'
     *
     * eg. foo(MyClass::class) => "#F\\foo|#K#C\\Foo.class"
     * 
     * foo($this->foo), foo('foobar')
     */
    @Nullable
    public static String getReferenceSignatureByFirstParameter(@NotNull FunctionReference functionReference, char trimKey) {
        String refSignature = functionReference.getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }

        PsiElement[] parameters = functionReference.getParameters();
        if(parameters.length == 0) {
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
                return StringUtils.stripStart(parameter.substring(4, parameter.length() - 6), "\\");
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

    /**
     * Mostly factory pattern doest not return a type, but eg in Doctrine getRepository we need to fallback to an interface
     */
    @NotNull
    public static Collection<? extends PhpNamedElement> mergeSignatureResults(@NotNull Collection<? extends PhpNamedElement> phpNamedElements, final @NotNull PhpNamedElement phpNamed) {

        Collection<PhpNamedElement> result = new ArrayList<>();
        result.add(phpNamed);

        // invalidate state; we dont know what to do
        if(!(phpNamed instanceof PhpClass)) {
            result.addAll(phpNamedElements);
            return result;
        }

        for (PhpNamedElement phpNamedElement : phpNamedElements) {
            if(phpNamedElement == null) {
                continue;
            }

            // nothing found
            if(!(phpNamedElement instanceof Method)) {
                result.add(phpNamedElement);
                continue;
            }

            // type are equal
            if(isPhpTypeEqual(phpNamedElement.getType(), (PhpClass) phpNamed)) {
                continue;
            }

            result.add(phpNamedElement);
        }

        return result;
    }

    private static boolean isPhpTypeEqual(@NotNull PhpType phpType, @NotNull PhpClass phpClass) {

        for (String s : phpType.getTypes()) {
            if(PhpElementsUtil.isInstanceOf(phpClass, s)) {
                return true;
            }
        }

        return false;
    }

    /**
     * We can have multiple types inside a TypeProvider; split them on "|" so that we dont get empty types
     *
     * #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.getRepository|
     * #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.getRepository
     */
    @NotNull
    public static Collection<? extends PhpNamedElement> getTypeSignature(@NotNull PhpIndex phpIndex, @NotNull String signature) {

        if (!signature.contains("|")) {
            return phpIndex.getBySignature(signature, null, 0);
        }

        Collection<PhpNamedElement> elements = new ArrayList<>();
        for (String s : signature.split("\\|")) {
            elements.addAll(phpIndex.getBySignature(s, null, 0));
        }

        return elements;
    }
}
