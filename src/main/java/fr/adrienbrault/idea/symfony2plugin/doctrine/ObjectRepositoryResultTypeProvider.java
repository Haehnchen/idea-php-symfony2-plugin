package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryResultTypeProvider implements PhpTypeProvider4 {
    private static MethodMatcher.CallToSignature[] FIND_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "find"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findOneBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findAll"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "find"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findOneBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findAll"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findBy"),
    };

    final static char TRIM_KEY = '\u0184';

    @Override
    public char getKey() {
        return '\u0152';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (!Settings.getInstance(e.getProject()).pluginEnabled) {
            return null;
        }

        // filter out method calls without parameter
        // $this->get('service_name')
        if(!PlatformPatterns
            .psiElement(PhpElementTypes.METHOD_REFERENCE)
            .withChild(PlatformPatterns
                .psiElement(PhpElementTypes.PARAMETER_LIST)
            ).accepts(e)) {

            return null;
        }

        MethodReference methodRef = (MethodReference) e;

        String refSignature = ((MethodReference)e).getSignature();
        if(StringUtil.isEmpty(refSignature)) {
            return null;
        }

        String methodRefName = methodRef.getName();
        if(null == methodRefName || !Arrays.asList(new String[] {"find", "findOneBy", "findAll", "findBy"}).contains(methodRefName)) {
            return null;
        }

        // at least one parameter is necessary on some finds
        PsiElement[] parameters = methodRef.getParameters();
        if(!methodRefName.equals("findAll")) {
            if(parameters.length == 0) {
                return null;
            }
        } else if(parameters.length != 0) {
            return null;
        }

        // we can get the repository name from the signature calls
        // #M#?#M#?#M#C\Foo\Bar\Controller\BarController.get?doctrine.getRepository?EntityBundle:User.find
        String repositorySignature = methodRef.getSignature();

        int lastRepositoryName = repositorySignature.lastIndexOf(ObjectRepositoryTypeProvider.TRIM_KEY);
        if(lastRepositoryName == -1) {
            return null;
        }

        repositorySignature = repositorySignature.substring(lastRepositoryName);
        int nextMethodCall = repositorySignature.indexOf('.' + methodRefName);
        if(nextMethodCall == -1) {
            return null;
        }

        repositorySignature = repositorySignature.substring(1, nextMethodCall);

        return new PhpType().add("#" + this.getKey() + refSignature + TRIM_KEY + repositorySignature);
    }

    @Override
    public PhpType complete(String expression, Project project) {
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

        Method method = getObjectRepositoryCall(phpNamedElementCollections);
        if(method == null) {
            return Collections.emptySet();
        }

        // we can also pipe php references signatures and resolve them here
        // overwrite parameter to get string value
        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if(parameter == null) {
            return Collections.emptySet();
        }

        PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
        if(phpClass == null) {
            return Collections.emptySet();
        }

        String name = method.getName();
        if(name.equals("findAll") || name.equals("findBy")) {
            method.getType().add(phpClass.getFQN() + "[]");
            return phpNamedElementCollections;
        }

        return PhpTypeProviderUtil.mergeSignatureResults(phpNamedElementCollections, phpClass);
    }

    private Method getObjectRepositoryCall(Collection<? extends PhpNamedElement> phpNamedElements) {
        for (PhpNamedElement phpNamedElement: phpNamedElements) {
            if(phpNamedElement instanceof Method && PhpElementsUtil.isMethodInstanceOf((Method) phpNamedElement, FIND_SIGNATURES)) {
                return (Method) phpNamedElement;
            }
        }

        return null;
    }
}
