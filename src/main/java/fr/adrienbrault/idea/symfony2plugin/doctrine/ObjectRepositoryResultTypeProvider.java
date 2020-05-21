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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolve "find*" and attach the entity from the getRepository method
 *
 * "$om->getRepository('\Foo\Bar')->find('foobar')->get<caret>Id()"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryResultTypeProvider implements PhpTypeProvider4 {
    private static final MethodMatcher.CallToSignature[] FIND_SIGNATURES = new MethodMatcher.CallToSignature[] {
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
        if (!(e instanceof MethodReference) || !Settings.getInstance(e.getProject()).pluginEnabled) {
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

        if (repositorySignature.startsWith("#K#C")) {
            repositorySignature = repositorySignature.substring(4);
        }

        if (repositorySignature.contains(".class")) {
            repositorySignature = repositorySignature.substring(0, repositorySignature.indexOf(".class"));
        }

        return new PhpType().add("#" + this.getKey() + refSignature + TRIM_KEY + repositorySignature);
    }

    @Nullable
    @Override
    public PhpType complete(String s, Project project) {
        int endIndex = s.lastIndexOf(TRIM_KEY);
        if(endIndex == -1) {
            return null;
        }

        String originalSignature = s.substring(0, endIndex);
        String parameter = s.substring(endIndex + 1);
        parameter = PhpTypeProviderUtil.getResolvedParameter(PhpIndex.getInstance(project), parameter);
        if(parameter == null) {
            return null;
        }

        PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
        if(phpClass == null) {
            return null;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(project);

        Collection<? extends PhpNamedElement> typeSignature = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);

        // ->getRepository(SecondaryMarket::class)->findAll() => "findAll", but only if its a instance of this method;
        // so non Doctrine method are already filtered
        Set<String> resolveMethods = getObjectRepositoryCall(typeSignature).stream()
            .map(PhpNamedElement::getName)
            .collect(Collectors.toSet());

        if (resolveMethods.isEmpty()) {
            return null;
        }

        PhpType phpType = new PhpType();

        resolveMethods.stream()
            .map(name -> name.equals("findAll") || name.equals("findBy") ? phpClass.getFQN() + "[]" : phpClass.getFQN())
            .collect(Collectors.toSet())
            .forEach(phpType::add);

        return phpType;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        return null;
    }

    @NotNull
    private Collection<Method> getObjectRepositoryCall(Collection<? extends PhpNamedElement> phpNamedElements) {
        Collection<Method> methods = new HashSet<>();
        for (PhpNamedElement phpNamedElement: phpNamedElements) {
            if(phpNamedElement instanceof Method && PhpElementsUtil.isMethodInstanceOf((Method) phpNamedElement, FIND_SIGNATURES)) {
                methods.add((Method) phpNamedElement);
            }
        }

        return methods;
    }
}
