package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention.YamlSuggestIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLScalar;

/**
 * foo:
 *  class: Foo
 *  arguments: [@<caret>]
 *  arguments:
 *      - @<caret>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlXmlServiceInstanceInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        @NotNull
        private final ProblemsHolder holder;

        @Nullable
        private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        private MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            // only match inside service definitions
            if(YamlHelper.isStringValue(psiElement) && YamlElementPatternHelper.getInsideKeyValue("services").accepts(psiElement)) {
                visitConstructor(psiElement);
                visitCall(psiElement);
            }

            super.visitElement(psiElement);
        }

        /**
         * class: FooClass
         * tags:
         *  - [ setFoo, [@args_bar] ]
         */
        private void visitCall(PsiElement psiElement) {
            PsiElement yamlScalar = psiElement.getContext();
            if(!(yamlScalar instanceof YAMLScalar)) {
                return;
            }

            YamlHelper.visitServiceCallArgument((YAMLScalar) yamlScalar, visitor -> {
                PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), visitor.getClassName(), getLazyServiceCollector(holder.getProject()));
                if(serviceClass == null) {
                    return;
                }

                Method method = serviceClass.findMethodByName(visitor.getMethod());
                if (method == null) {
                    return;
                }

                YamlXmlServiceInstanceInspection.registerInstanceProblem(psiElement, holder, visitor.getParameterIndex(), method, getLazyServiceCollector(holder.getProject()));
            });
        }

        /**
         * foo:
         *  class: Foo
         *  arguments: [@<caret>]
         *  arguments:
         *      - @<caret>
         */
        private void visitConstructor(PsiElement psiElement) {
            ServiceTypeHint methodTypeHint = ServiceContainerUtil.getYamlConstructorTypeHint(psiElement, getLazyServiceCollector(holder.getProject()));
            if(methodTypeHint == null) {
                return;
            }

            registerInstanceProblem(psiElement, holder, methodTypeHint.getIndex(), methodTypeHint.getMethod(), getLazyServiceCollector(holder.getProject()));
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(@NotNull Project project) {
            return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
        }
    }

    static void registerInstanceProblem(@NotNull PsiElement psiElement, @NotNull ProblemsHolder holder, int parameterIndex, @NotNull Method constructor, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        String serviceName = getServiceName(psiElement);
        if(StringUtils.isBlank(serviceName)) {
            return;
        }

        PhpClass serviceParameterClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), getServiceName(psiElement), lazyServiceCollector);
        if(serviceParameterClass == null) {
            return;
        }

        Parameter[] constructorParameter = constructor.getParameters();
        if(parameterIndex >= constructorParameter.length) {
            return;
        }

        PhpClass expectedClass = PhpElementsUtil.getClassInterface(holder.getProject(), constructorParameter[parameterIndex].getDeclaredType().toString());
        if(expectedClass == null) {
            return;
        }

        if(!PhpElementsUtil.isInstanceOf(serviceParameterClass, expectedClass)) {
            holder.registerProblem(
                psiElement,
                "Expect instance of: " + expectedClass.getPresentableFQN(),
                new YamlSuggestIntentionAction(expectedClass.getFQN(), psiElement)
            );
        }
    }

    private static String getServiceName(PsiElement psiElement) {
        return YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(psiElement));
    }
}
