package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict.YamlCreateServiceArgumentsCallback;
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict.YamlUpdateArgumentServicesCallback;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlServiceArgumentInspection extends LocalInspectionTool {

    public static final String[] INVALID_KEYS = new String[]{"parent", "factory_class", "factory_service", "factory_method", "abstract", "factory", "autowrite"};
    private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(psiFile.getFileType() != YAMLFileType.YML || !Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        for (ServiceActionUtil.ServiceYamlContainer serviceYamlContainer : ServiceActionUtil.getYamlContainerServiceArguments(psiFile)) {

            // we dont support parent services for now
            if(!isValidService(serviceYamlContainer)) {
                continue;
            }

            List<String> yamlMissingArgumentTypes = ServiceActionUtil.getYamlMissingArgumentTypes(psiFile.getProject(), serviceYamlContainer, false, getLazyServiceCollector(psiFile.getProject()));
            if(yamlMissingArgumentTypes != null && yamlMissingArgumentTypes.size() > 0) {
                holder.registerProblem(serviceYamlContainer.getServiceKey().getFirstChild(), "Missing argument", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new YamlArgumentQuickfix());
            }
        }

        this.lazyServiceCollector = null;

        return super.buildVisitor(holder, isOnTheFly);
    }

    private boolean isValidService(ServiceActionUtil.ServiceYamlContainer serviceYamlContainer) {

        Set<String> keySet = YamlHelper.getKeySet(serviceYamlContainer.getServiceKey());
        if(keySet == null) {
            return true;
        }

        for(String s: INVALID_KEYS) {
            if(keySet.contains(s)) {
                return false;
            }
        }

        return true;
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

            YAMLKeyValue argumentsKeyValue = YamlHelper.getYamlKeyValue((YAMLKeyValue) serviceKeyValue, "arguments");

            // there is no "arguments" key so provide one
            if(argumentsKeyValue == null) {

                ServiceActionUtil.ServiceYamlContainer serviceYamlContainer = ServiceActionUtil.ServiceYamlContainer.create((YAMLKeyValue) serviceKeyValue);
                List<String> yamlMissingArgumentTypes = ServiceActionUtil.getYamlMissingArgumentTypes(project, serviceYamlContainer, false, new ContainerCollectionResolver.LazyServiceCollector(project));

                if(yamlMissingArgumentTypes == null) {
                    return;
                }

                ServiceActionUtil.fixServiceArgument(project, yamlMissingArgumentTypes, new YamlCreateServiceArgumentsCallback((YAMLKeyValue) serviceKeyValue));

                return;
            }

            // update service
            ServiceActionUtil.ServiceYamlContainer serviceYamlContainer = ServiceActionUtil.ServiceYamlContainer.create((YAMLKeyValue) serviceKeyValue);
            List<String> yamlMissingArgumentTypes = ServiceActionUtil.getYamlMissingArgumentTypes(project, serviceYamlContainer, false, new ContainerCollectionResolver.LazyServiceCollector(project));
            if(yamlMissingArgumentTypes == null) {
                return;
            }

            ServiceActionUtil.fixServiceArgument(project, yamlMissingArgumentTypes, new YamlUpdateArgumentServicesCallback(
                project,
                argumentsKeyValue,
                (YAMLKeyValue) serviceKeyValue
            ));
        }

    }

    private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
        return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
    }

}
