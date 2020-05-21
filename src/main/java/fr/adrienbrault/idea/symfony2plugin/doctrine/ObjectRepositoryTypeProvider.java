package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryTypeProvider implements PhpTypeProvider4 {
    private static final MethodMatcher.CallToSignature[] GET_REPOSITORIES_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ManagerRegistry", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectManager", "getRepository"),
    };

    final public static char TRIM_KEY = '\u0185';

    @Override
    public char getKey() {
        return '\u0151';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (!Settings.getInstance(e.getProject()).pluginEnabled) {
            return null;
        }

        if(!(e instanceof MethodReference) || !PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "getRepository")) {
            return null;
        }


        String refSignature = ((MethodReference)e).getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }

        String signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter((MethodReference) e, TRIM_KEY);
        return signature == null ? null : new PhpType().add("#" + this.getKey() + signature);
    }

    @Nullable
    @Override
    public PhpType complete(String s, Project project) {
        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        // get back our original call
        int endIndex = expression.lastIndexOf(TRIM_KEY);
        if(endIndex == -1) {
            return Collections.emptySet();
        }

        String originalSignature = expression.substring(0, endIndex);
        String parameter = expression.substring(endIndex + 1);

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);
        if(phpNamedElementCollections.size() == 0) {
            return Collections.emptySet();
        }

        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Method)) {
            return phpNamedElementCollections;
        }

        if (!PhpElementsUtil.isMethodInstanceOf((Method) phpNamedElement, GET_REPOSITORIES_SIGNATURES)) {
            return phpNamedElementCollections;
        }

        // we can also pipe php references signatures and resolve them here
        // overwrite parameter to get string value
        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if(parameter == null) {
            return phpNamedElementCollections;
        }

        PhpClass phpClass = EntityHelper.getEntityRepositoryClass(project, parameter);
        if(phpClass == null) {
            // self add :)
            return phpNamedElementCollections;
        }

        return PhpTypeProviderUtil.mergeSignatureResults(phpNamedElementCollections, phpClass);
    }
}
