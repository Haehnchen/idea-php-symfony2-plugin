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
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * \Doctrine\Common\Persistence\ObjectManager::find('REPOSITORY', $foo)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectManagerFindTypeProvider implements PhpTypeProvider4 {

    final static char TRIM_KEY = '\u0183';

    @Override
    public char getKey() {
        return '\u0153';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if(!(e instanceof MethodReference methodReference)) {
            return null;
        }

        Project project = e.getProject();
        if (!Settings.getInstance(project).pluginEnabled || !Settings.getInstance(project).featureTwigIcon) {
            return null;
        }

        if(!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "find")) {
            return null;
        }

        String refSignature = methodReference.getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }

        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length >= 2) {
            String signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter((MethodReference) e, TRIM_KEY);
            if(signature != null) {
                return new PhpType().add("#" + this.getKey() + signature);
            }
        }

        return null;
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
        if(phpNamedElementCollections.isEmpty()) {
            return Collections.emptySet();
        }

        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Method)) {
            return Collections.emptySet();
        }

        if (!(
            PhpElementsUtil.isMethodInstanceOf((Method) phpNamedElement, "\\Doctrine\\Common\\Persistence\\ObjectManager", "find") ||
            PhpElementsUtil.isMethodInstanceOf((Method) phpNamedElement, "\\Doctrine\\Persistence\\ObjectManager", "find")
        )) {
            return Collections.emptySet();
        }

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if(parameter == null) {
            return Collections.emptySet();
        }

        PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
        if(phpClass == null) {
            return Collections.emptySet();
        }

        return PhpTypeProviderUtil.mergeSignatureResults(phpNamedElementCollections, phpClass);
    }
}
