package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class Symfony2InterfacesUtil {

    public boolean isContainerGetCall(PsiElement e) {
        return isCallTo(e, new Method[] {
            getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "get"),
            getClassMethod(e.getProject(), "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "get"),
        });
    }

    public boolean isContainerGetCall(Method e) {
        return isCallTo(e, new Method[] {
            getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "get"),
            getClassMethod(e.getProject(), "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "get"),
        });
    }

    public boolean isTemplatingRenderCall(PsiElement e) {
        return isCallTo(e, new Method[] {
            getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\Templating\\EngineInterface", "render"),
            getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\Templating\\StreamingEngineInterface", "stream"),
            getInterfaceMethod(e.getProject(), "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\EngineInterface", "renderResponse"),
            getClassMethod(e.getProject(), "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "render"),
            getClassMethod(e.getProject(), "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "renderView"),
            getClassMethod(e.getProject(), "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "stream"),
        });
    }

    public boolean isTranslatorCall(PsiElement e) {
        return isCallTo(e, new Method[] {
                getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\Translation\\TranslatorInterface", "trans"),
                getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\Translation\\TranslatorInterface", "transChoice"),
        });
    }

    public boolean isGetRepositoryCall(Method e) {
        return isCallTo(e, new Method[] {
            getInterfaceMethod(e.getProject(), "\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository"),
            getInterfaceMethod(e.getProject(), "\\Doctrine\\Common\\Persistence\\ObjectManager", "getRepository"),
        });
    }

    public boolean isObjectRepositoryCall(Method e) {
        return isCallTo(e, new Method[] {
            getInterfaceMethod(e.getProject(), "\\Doctrine\\Common\\Persistence\\ObjectRepository", "find"),
            getInterfaceMethod(e.getProject(), "\\Doctrine\\Common\\Persistence\\ObjectRepository", "findOneBy"),
            getInterfaceMethod(e.getProject(), "\\Doctrine\\Common\\Persistence\\ObjectRepository", "findAll"),
            getInterfaceMethod(e.getProject(), "\\Doctrine\\Common\\Persistence\\ObjectRepository", "findBy"),
        });
    }

    public boolean isFormBuilderFormTypeCall(PsiElement e) {
        List<Method> methods = getCallToSignatureInterfaceMethods(e, getFormBuilderInterface());
        return isCallTo(e, methods.toArray( new Method[methods.size()]));
    }

    protected boolean isCallTo(PsiElement e, Method expectedMethod) {
        return isCallTo(e, new Method[] { expectedMethod });
    }

    protected boolean isCallTo(PsiElement e, Method[] expectedMethods) {
        return isCallTo(e, expectedMethods, 1);
    }

    protected boolean isCallTo(Method e, Method[] expectedMethods) {

        PhpClass methodClass = e.getContainingClass();
        if(methodClass == null) {
            return false;
        }

        for (Method expectedMethod : expectedMethods) {

            // @TODO: its stuff from beginning times :)
            if(expectedMethod == null) {
                continue;
            }

            PhpClass containingClass = expectedMethod.getContainingClass();
            if (containingClass != null && expectedMethod.getName().equals(e.getName()) && isInstanceOf(methodClass, containingClass)) {
                return true;
            }
        }

        return false;
    }


    protected boolean isCallTo(PsiElement e, Method[] expectedMethods, int deepness) {
        if (!(e instanceof MethodReference)) {
            return false;
        }

        MethodReference methodRef = (MethodReference) e;

        // resolve is also called on invalid php code like "use <xxx>"
        // so double check the method name before resolve the method
        if(!isMatchingMethodName(methodRef, expectedMethods)) {
            return false;
        }

        PsiReference psiReference = methodRef.getReference();
        if (null == psiReference) {
            return false;
        }

        Method[] multiResolvedMethod = getMultiResolvedMethod(psiReference);
        if(multiResolvedMethod == null) {
            return false;
        }

        for (Method method : multiResolvedMethod) {

            PhpClass methodClass = method.getContainingClass();
            if(methodClass == null) {
                continue;
            }

            for (Method expectedMethod : expectedMethods) {

                // @TODO: its stuff from beginning times :)
                if(expectedMethod == null) {
                    continue;
                }

                PhpClass containingClass = expectedMethod.getContainingClass();
                if (null != containingClass && expectedMethod.getName().equals(method.getName()) && isInstanceOf(methodClass, containingClass)) {
                    return true;
                }
            }

        }

        return false;
    }

    /**
     * Single resolve doesnt work if we have non unique class names in project context,
     * so try a multiResolve
     */
    @Nullable
    public static Method[] getMultiResolvedMethod(PsiReference psiReference) {

        // class be unique in normal case, so try this first
        PsiElement resolvedReference = psiReference.resolve();
        if (resolvedReference instanceof Method) {
            return new Method[] { (Method) resolvedReference };
        }

        // try multiResolve if class exists twice in project
        if(psiReference instanceof PsiPolyVariantReference) {
            Collection<Method> methods = new HashSet<Method>();
            for(ResolveResult resolveResult : ((PsiPolyVariantReference) psiReference).multiResolve(false)) {
                PsiElement element = resolveResult.getElement();
                if(element instanceof Method) {
                    methods.add((Method) element);
                }
            }

            if(methods.size() > 0) {
                return methods.toArray(new Method[methods.size()]);
            }

        }

        return null;
    }

    protected boolean isMatchingMethodName(MethodReference methodRef, Method[] expectedMethods) {
        for (Method expectedMethod : Arrays.asList(expectedMethods)) {
            if(expectedMethod != null && expectedMethod.getName().equals(methodRef.getName())) {
                return true;
            }
        }

        return false;
    }

    @Deprecated
    @Nullable
    public static String getFirstArgumentStringValue(MethodReference e) {
        String stringValue = null;

        PsiElement[] parameters = e.getParameters();
        if (parameters.length > 0 && parameters[0] instanceof StringLiteralExpression) {
            stringValue = ((StringLiteralExpression) parameters[0]).getContents();
        }

        return stringValue;
    }

    @Nullable
    protected Method getInterfaceMethod(Project project, String interfaceFQN, String methodName) {

        Collection<PhpClass> interfaces = PhpIndex.getInstance(project).getInterfacesByFQN(interfaceFQN);

        if (interfaces.size() < 1) {
            return null;
        }

        return findClassMethodByName(interfaces.iterator().next(), methodName);
    }

    @Nullable
    protected Method getClassMethod(Project project, String classFQN, String methodName) {
        return PhpElementsUtil.getClassMethod(project, classFQN, methodName);
    }

    @Nullable
    protected Method findClassMethodByName(PhpClass phpClass, String methodName) {
        return PhpElementsUtil.getClassMethod(phpClass, methodName);
    }

    protected boolean isImplementationOfInterface(PhpClass phpClass, PhpClass phpInterface) {
        if (phpClass == phpInterface) {
            return true;
        }

        for (PhpClass implementedInterface : phpClass.getImplementedInterfaces()) {
            if (isImplementationOfInterface(implementedInterface, phpInterface)) {
                return true;
            }
        }

        if (null == phpClass.getSuperClass()) {
            return false;
        }

        return isImplementationOfInterface(phpClass.getSuperClass(), phpInterface);
    }

    public boolean isInstanceOf(PhpClass subjectClass, String expectedClass) {

        PhpClass instanceClass = PhpElementsUtil.getClassInterface(subjectClass.getProject(), expectedClass);

        if(instanceClass == null) {
            return false;
        }

        return isInstanceOf(subjectClass, instanceClass);

    }

    public boolean isInstanceOf(Project project, String subjectClass, String expectedClass) {

        PhpClass subjectPhpClass = PhpElementsUtil.getClassInterface(project, subjectClass);
        if(subjectPhpClass == null) {
            return false;
        }

        return isInstanceOf(subjectPhpClass, expectedClass);

    }

    public boolean isInstanceOf(@NotNull PhpClass subjectClass, @NotNull PhpClass expectedClass) {
        if (subjectClass == expectedClass) {
            return true;
        }

        if (expectedClass.isInterface()) {
            return isImplementationOfInterface(subjectClass, expectedClass);
        }

        if (null == subjectClass.getSuperClass()) {
            return false;
        }

        return isInstanceOf(subjectClass.getSuperClass(), expectedClass);
    }

    public boolean isCallTo(MethodReference e, String ClassInterfaceName, String methodName) {
        return isCallTo((PsiElement) e, ClassInterfaceName, methodName);
    }

    /**
     * @deprecated isCallTo with MethodReference
     */
    public boolean isCallTo(PsiElement e, String ClassInterfaceName, String methodName) {

        // we need a full fqn name
        if(ClassInterfaceName.contains("\\") && !ClassInterfaceName.startsWith("\\")) {
            ClassInterfaceName = "\\" + ClassInterfaceName;
        }

        return isCallTo(e, new Method[] {
            getInterfaceMethod(e.getProject(), ClassInterfaceName, methodName),
            getClassMethod(e.getProject(), ClassInterfaceName, methodName),
        });
    }

    public boolean isCallTo(Method e, String ClassInterfaceName, String methodName) {

        // we need a full fqn name
        if(ClassInterfaceName.contains("\\") && !ClassInterfaceName.startsWith("\\")) {
            ClassInterfaceName = "\\" + ClassInterfaceName;
        }

        return isCallTo(e, new Method[] {
            getInterfaceMethod(e.getProject(), ClassInterfaceName, methodName),
            getClassMethod(e.getProject(), ClassInterfaceName, methodName),
        });
    }

    private List<Method> getCallToSignatureInterfaceMethods(PsiElement e, Collection<MethodMatcher.CallToSignature> signatures) {
        List<Method> methods = new ArrayList<Method>();
        for(MethodMatcher.CallToSignature signature: signatures) {
            Method method = getInterfaceMethod(e.getProject(), signature.getInstance(), signature.getMethod());
            if(method != null) {
                methods.add(method);
            }
        }
        return methods;
    }

    public static Collection<MethodMatcher.CallToSignature> getFormBuilderInterface() {
        Collection<MethodMatcher.CallToSignature> signatures = new ArrayList<MethodMatcher.CallToSignature>();

        signatures.add(new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormBuilderInterface", "add"));
        signatures.add(new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormBuilderInterface", "create"));
        signatures.add(new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormInterface", "add"));
        signatures.add(new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormInterface", "create"));

        return signatures;
    }

}
