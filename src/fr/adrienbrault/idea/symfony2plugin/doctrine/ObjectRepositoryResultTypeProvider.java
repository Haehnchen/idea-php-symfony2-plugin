package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.DumbService;
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
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryResultTypeProvider implements PhpTypeProvider2 {

    final static char TRIM_KEY = '\u0180';

    @Override
    public char getKey() {
        return '\u0152';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled || !Settings.getInstance(e.getProject()).objectRepositoryResultTypeProvider) {
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

        // @TODO: find the previously defined type instead of try it on the parameter, we now can rely on it!
        // find the called repository name on method before
        if(!(methodRef.getFirstChild() instanceof MethodReference)) {
            return null;
        }

        String repositoryName = Symfony2InterfacesUtil.getFirstArgumentStringValue((MethodReference) methodRef.getFirstChild());

        if (repositoryName != null) {
            return refSignature + TRIM_KEY + repositoryName;
        }

        return null;

    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        // get back our original call
        String originalSignature = expression.substring(0, expression.lastIndexOf(TRIM_KEY));
        String parameter = expression.substring(expression.lastIndexOf(TRIM_KEY) + 1);

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature(originalSignature, null, 0);
        if(phpNamedElementCollections.size() == 0) {
            return Collections.emptySet();
        }

        Method method = (Method) phpNamedElementCollections.iterator().next();
        if (!new Symfony2InterfacesUtil().isObjectRepositoryCall(method)) {
            return Arrays.asList(method);
        }

        PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
        if(phpClass == null) {
            return Arrays.asList(method);
        }

        if(method.getName().equals("findAll") || method.getName().equals("findBy")) {
            method.getType().add(phpClass.getFQN() + "[]");
            return Arrays.asList(method);
        }

        return Arrays.asList(phpClass);
    }

}
