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
        PhpTypeProviderUtil.SignatureSplit sig = PhpTypeProviderUtil.resolveSignatureSplit(expression, TRIM_KEY, project);
        if (sig == null) {
            return Collections.emptySet();
        }

        PhpNamedElement phpNamedElement = sig.elements().iterator().next();
        if (!(phpNamedElement instanceof Method)) {
            return sig.elements();
        }

        String parameter = PhpTypeProviderUtil.getResolvedParameter(sig.phpIndex(), sig.parameter());
        if (parameter == null) {
            return sig.elements();
        }

        // finally search the classes
        if (PhpElementsUtil.isMethodInstanceOf((Method) phpNamedElement, ServiceContainerUtil.SERVICE_GET_SIGNATURES)) {
            ContainerService containerService = ContainerCollectionResolver.getService(project, parameter);

            if (containerService != null) {
                Collection<PhpNamedElement> phpClasses = new HashSet<>();

                for (String s : containerService.getClassNames()) {
                    phpClasses.addAll(PhpIndex.getInstance(project).getAnyByFQN(s));
                }

                return phpClasses;
            }
        }

        return sig.elements();
    }
}
