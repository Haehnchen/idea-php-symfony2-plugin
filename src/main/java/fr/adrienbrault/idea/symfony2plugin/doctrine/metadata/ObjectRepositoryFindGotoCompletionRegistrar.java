package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelFieldLookupElement;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelInterface;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryFindGotoCompletionRegistrar implements GotoCompletionRegistrar {
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {

        // "@var $om \Doctrine\Common\Persistence\ObjectManager"
        // "$om->getRepository('Foo\Bar')->" + s + "(['foo' => 'foo', '<caret>' => 'foo'])"
        registrar.register(PhpElementsUtil.getParameterListArrayValuePattern(), psiElement -> {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(context, 0)
                .withSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findOneBy")
                .withSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findBy")
                .withSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "count")
                .withSignature("\\Doctrine\\Persistence\\ObjectRepository", "findOneBy")
                .withSignature("\\Doctrine\\Persistence\\ObjectRepository", "findBy")
                .withSignature("\\Doctrine\\Persistence\\ObjectRepository", "count")
                .match();

            if(methodMatchParameter != null) {
                MethodReference methodReference = methodMatchParameter.getMethodReference();
                Project project = psiElement.getProject();

                // extract from type provide on completion:
                // $foo->getRepository('MODEL')->findBy()
                Collection<PhpClass> phpClasses = PhpElementsUtil.getClassFromPhpTypeSetArrayClean(project, methodReference.getType().getTypes());

                // resolve every direct repository instance $this->findBy()
                // or direct repository instance $repository->findBy()
                if(phpClasses.isEmpty()) {
                    PhpExpression classReference = methodReference.getClassReference();
                    if(classReference != null) {
                        PhpType type = classReference.getType();
                        for (String s : type.getTypes()) {
                            // dont visit type providers
                            if(PhpType.isUnresolved(s)) {
                                continue;
                            }

                            for (DoctrineModelInterface doctrineModel : DoctrineMetadataUtil.findMetadataModelForRepositoryClass(project, s)) {
                                phpClasses.addAll(PhpElementsUtil.getClassesInterface(project, doctrineModel.getClassName()));
                            }
                        }
                    }
                }

                if(phpClasses.isEmpty()) {
                    return null;
                }

                return new MyArrayFieldMetadataGotoCompletionRegistrar(psiElement, phpClasses);
            }

            return null;
        });
    }

    private static class MyArrayFieldMetadataGotoCompletionRegistrar extends GotoCompletionProvider {
        @NotNull
        private final Collection<PhpClass> phpClasses;

        MyArrayFieldMetadataGotoCompletionRegistrar(@NotNull PsiElement element, @NotNull Collection<PhpClass> phpClasses) {
            super(element);
            this.phpClasses = phpClasses;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            List<LookupElement> results = new ArrayList<>();

            phpClasses.forEach(phpClass ->
                results.addAll(EntityHelper.getModelFields(phpClass).stream()
                    .map((Function<DoctrineModelField, LookupElement>) DoctrineModelFieldLookupElement::new)
                    .collect(Collectors.toList())
                )
            );

            return results;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String content = GotoCompletionUtil.getTextValueForElement(element);
            if(content == null) {
                return Collections.emptyList();
            }

            Collection<PsiElement> results = new ArrayList<>();

            phpClasses.forEach(phpClass ->
                results.addAll(Arrays.asList(EntityHelper.getModelFieldTargets(phpClass, content)))
            );

            return results;
        }
    }
}
