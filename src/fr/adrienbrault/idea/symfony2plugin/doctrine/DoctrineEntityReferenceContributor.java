package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineEntityReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
                PlatformPatterns.psiElement(StringLiteralExpression.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 0)
                            .withSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository")
                            .withSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "getRepository")
                            .match();

                        // try on resolved method
                        if(methodMatchParameter == null) {
                            methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(psiElement)
                                .withSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository")
                                .withSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "getRepository")
                                .match();
                        }

                        if(methodMatchParameter == null) {
                            return new PsiReference[0];
                        }

                        DoctrineTypes.Manager manager = EntityHelper.getManager(methodMatchParameter.getMethodReference());
                        if(manager != null) {
                            return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement, manager) };
                        }

                        return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement) };
                    }
                }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 0)
                        .withSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "find")
                        .match();

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    DoctrineTypes.Manager manager = EntityHelper.getManager(methodMatchParameter.getMethodReference());
                    if(manager != null) {
                        return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement, manager) };
                    }

                    return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement) };
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(psiElement, 0)
                        .withSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findOneBy")
                        .withSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findBy")
                        .match();

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    Collection<PhpClass> phpClasses = PhpElementsUtil.getClassFromPhpTypeSet(psiElement.getProject(), methodMatchParameter.getMethodReference().getType().getTypes());
                    return new PsiReference[]{ new ModelFieldReference((StringLiteralExpression) psiElement, phpClasses)};
                }
            }
        );


    }

}
