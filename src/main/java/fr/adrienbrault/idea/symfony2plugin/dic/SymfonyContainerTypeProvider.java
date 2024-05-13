package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyContainerTypeProvider implements PhpTypeProvider4 {
    private static final char TRIM_KEY = '\u0182';

    @Override
    public char getKey() {
        return '\u0150';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        // container calls are only on "get" methods
        if(!(e instanceof MethodReference methodReference)) {
            return null;
        }

        Project project = e.getProject();
        if (!Settings.getInstance(project).pluginEnabled || !Settings.getInstance(project).featureTypeProvider) {
            return null;
        }

        // container calls are only on "get" methods
        if(!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "get")) {
            return null;
        }

        String signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(methodReference, TRIM_KEY);
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
        // since phpstorm 7.1.2 we need to validate this
        int endIndex = expression.lastIndexOf(TRIM_KEY);
        if(endIndex == -1) {
            return Collections.emptySet();
        }

        String originalSignature = expression.substring(0, endIndex);
        String parameter = expression.substring(endIndex + 1);

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);
        if(phpNamedElementCollections.isEmpty()) {
            return Collections.emptySet();
        }

        // get first matched item
        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Method)) {
            return phpNamedElementCollections;
        }

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if(parameter == null) {
            return phpNamedElementCollections;
        }

        // finally search the classes
        if(PhpElementsUtil.isMethodInstanceOf((Method) phpNamedElement, ServiceContainerUtil.SERVICE_GET_SIGNATURES)) {
            ContainerService containerService = ContainerCollectionResolver.getService(project, parameter);

            if(containerService != null) {
                Collection<PhpNamedElement> phpClasses = new HashSet<>();

                for (String s : containerService.getClassNames()) {
                    phpClasses.addAll(PhpIndex.getInstance(project).getAnyByFQN(s));
                }

                return phpClasses;
            }
        }

        return phpNamedElementCollections;
    }
}
