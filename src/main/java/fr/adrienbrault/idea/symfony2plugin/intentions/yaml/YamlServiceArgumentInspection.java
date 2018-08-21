package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import com.intellij.codeInspection.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlServiceArgumentInspection extends LocalInspectionTool {

    public static final String[] INVALID_KEYS = new String[]{
        "parent", "factory_class", "factory_service",
        "factory_method", "abstract", "factory",
        "resource", "exclude", "alias"
    };

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {

        @NotNull
        private final ProblemsHolder problemsHolder;
        private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        MyPsiElementVisitor(@NotNull ProblemsHolder problemsHolder) {
            this.problemsHolder = problemsHolder;
        }

        @Override
        public void visitFile(PsiFile file) {
            if(!(file instanceof YAMLFile)) {
                return;
            }

            for (ServiceActionUtil.ServiceYamlContainer serviceYamlContainer : ServiceActionUtil.getYamlContainerServiceArguments((YAMLFile) file)) {

                // we dont support parent services for now
                if("_defaults".equalsIgnoreCase(serviceYamlContainer.getServiceKey().getKeyText()) || !isValidService(serviceYamlContainer)) {
                    continue;
                }

                List<String> yamlMissingArgumentTypes = ServiceActionUtil.getYamlMissingArgumentTypes(problemsHolder.getProject(), serviceYamlContainer, false, getLazyServiceCollector(problemsHolder.getProject()));
                if(yamlMissingArgumentTypes.size() > 0) {
                    problemsHolder.registerProblem(serviceYamlContainer.getServiceKey().getFirstChild(), "Missing argument", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new YamlArgumentQuickfix());
                }
            }

            this.lazyServiceCollector = null;
        }

        private boolean isValidService(ServiceActionUtil.ServiceYamlContainer serviceYamlContainer) {
            YAMLKeyValue serviceKey = serviceYamlContainer.getServiceKey();

            Set<String> keySet = YamlHelper.getKeySet(serviceKey);
            if(keySet == null) {
                return true;
            }

            for(String s: INVALID_KEYS) {
                if(keySet.contains(s)) {
                    return false;
                }
            }

            // check autowire scope
            Boolean serviceAutowire = YamlHelper.getYamlKeyValueAsBoolean(serviceKey, "autowire");
            if(serviceAutowire != null) {
                // use service scope for autowire
                if(serviceAutowire) {
                    return false;
                }
            } else {
                // find file scope defaults
                // defaults: [autowire: true]
                YAMLMapping key = serviceKey.getParentMapping();
                if(key != null) {
                    YAMLKeyValue defaults = YamlHelper.getYamlKeyValue(key, "_defaults");
                    if(defaults != null) {
                        Boolean autowire = YamlHelper.getYamlKeyValueAsBoolean(defaults, "autowire");
                        if(autowire != null && autowire) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
            return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
        }
    }

    private static class YamlArgumentQuickfix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Symfony: Yaml Argument";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Symfony";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
            final PsiElement serviceKeyValue = problemDescriptor.getPsiElement().getParent();

            if(!(serviceKeyValue instanceof YAMLKeyValue)) {
                return;
            }

            WriteCommandAction.writeCommandAction(project).withName("Service Update").run(() -> {
                ServiceActionUtil.fixServiceArgument((YAMLKeyValue) serviceKeyValue);
            });
        }
    }
}
