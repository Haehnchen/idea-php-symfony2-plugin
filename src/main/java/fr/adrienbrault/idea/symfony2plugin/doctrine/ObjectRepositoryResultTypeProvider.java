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

import java.util.*;
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

    private static Set<String> MANAGED_FIND_METHOD = new HashSet<String>() {{
        add("find");
        add("findOneBy");
        add("findAll");
        add("findBy");
    }};

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

        if(null == methodRefName || (!Arrays.asList(new String[] {"find", "findAll"}).contains(methodRefName) && !methodRefName.startsWith("findOneBy") && !methodRefName.startsWith("findBy"))) {
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

        Collection<? extends PhpNamedElement> typeSignature = getTypeSignatureMagic(phpIndex, originalSignature);

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
            .map(name -> name.equals("findAll") || name.startsWith("findBy") ? phpClass.getFQN() + "[]" : phpClass.getFQN())
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

    /**
     * We can have multiple types inside a TypeProvider; split them on "|" so that we dont get empty types
     *
     * #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.getRepository|
     * #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.getRepository
     */
    @NotNull
    private static Collection<? extends PhpNamedElement> getTypeSignatureMagic(@NotNull PhpIndex phpIndex, @NotNull String signature) {
        // magic method resolving; we need to have the ObjectRepository method which does not exists for magic methods, so strip it
        // #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.findByName => findBy
        // #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.findOneBy => findOne
        Collection<PhpNamedElement> elements = new HashSet<>();
        for (String s : signature.split("\\|")) {
            int i = s.lastIndexOf(".");
            if (i > 0) {
                // method already exists in repository use it
                for (PhpNamedElement phpNamedElement : phpIndex.getBySignature(s, null, 0)) {
                    if (phpNamedElement instanceof Method) {
                        if (MANAGED_FIND_METHOD.contains(phpNamedElement.getName())) {
                            continue;
                        }

                        // we got into the repository itself so stop here; was not overwritten by repository class
                        PhpClass containingClass = ((Method) phpNamedElement).getContainingClass();
                        if (PhpElementsUtil.isEqualClassName(containingClass, "\\Doctrine\\Persistence\\ObjectRepository") || PhpElementsUtil.isEqualClassName(containingClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository")) {
                            continue;
                        }

                        return Collections.emptyList();
                    }
                }

                // strip field name from the method name "findOneByName" => "findOneBy"
                String substring = s.substring(i + 1);
                if (substring.startsWith("findOneBy")) {
                    s = s.substring(0, i + 1) + "findOneBy";
                } else if(substring.startsWith("findBy")) {
                    s = s.substring(0, i + 1) + "findBy";
                }
            }

            elements.addAll(phpIndex.getBySignature(s, null, 0));
        }

        return elements;
    }
}
