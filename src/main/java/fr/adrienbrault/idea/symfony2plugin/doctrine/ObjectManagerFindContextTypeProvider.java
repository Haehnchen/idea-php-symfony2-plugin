package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelInterface;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * $repository->find()->get<caret>Id();
 * $this->repository->find()->ge<caret>tId();
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectManagerFindContextTypeProvider implements PhpTypeProvider4 {

    final private static char TRIM_KEY = '\u0182';

    @Override
    public char getKey() {
        return '\u0173';
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

        String signature = null;
        PhpPsiElement firstPsiChild = methodRef.getFirstPsiChild();

        // reduce supported scope here; by checking via "instanceof"
        if (firstPsiChild instanceof Variable) {
            signature = ((Variable) firstPsiChild).getSignature();
        } else if(firstPsiChild instanceof PhpReference) {
            signature = ((PhpReference) firstPsiChild).getSignature();
        }

        if (signature == null || StringUtils.isBlank(signature)) {
            return null;
        }

        return new PhpType().add("#" + this.getKey() + signature + TRIM_KEY + methodRefName);
    }

    @Nullable
    @Override
    public PhpType complete(String s, Project project) {
        String[] split = s.substring(2).split(String.valueOf(TRIM_KEY));
        if (split.length < 2) {
            return null;
        }

        String signature = split[0];
        String methodName = split[1];

        Collection<PhpClass> repositoryClasses = new HashSet<>();

        // collect the instances of the type before our call; which we extract on "getType()"
        for (PhpNamedElement phpNamedElement : PhpIndex.getInstance(project).getBySignature(signature)) {
            // #C\Foo\BarRepository
            if (phpNamedElement instanceof PhpClass) {
                repositoryClasses.add((PhpClass) phpNamedElement);
                continue;
            }

            // resolve the the previous type
            for (String type : phpNamedElement.getType().filterPrimitives().getTypes()) {
                repositoryClasses.addAll(PhpIndex.getInstance(project).getAnyByFQN(type));
            }
        }

        // only all repository class
        Set<PhpClass> collect = repositoryClasses.stream()
            .filter(phpClass -> PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository") || PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Persistence\\ObjectRepository"))
            .collect(Collectors.toSet());

        if (collect.isEmpty()) {
            return null;
        }

        // based on repositoryClass find the Entity which as defined it via metadata
        Set<String> types = new HashSet<>();
        for (PhpClass phpClass : collect) {
            Collection<DoctrineModelInterface> phpClass1 = DoctrineMetadataUtil.findMetadataModelForRepositoryClass(project, phpClass.getPresentableFQN());

            for (DoctrineModelInterface doctrineModel : phpClass1) {
                for (PhpClass aClass : PhpElementsUtil.getClassesInterface(project, doctrineModel.getClassName())) {
                    types.add(methodName.equals("findAll") || methodName.startsWith("findBy") ? aClass.getFQN() + "[]" : aClass.getFQN());
                }
            }
        }

        if (types.isEmpty()) {
            return null;
        }

        PhpType phpType = new PhpType();
        types.forEach(phpType::add);
        return phpType;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        return null;
    }
}
