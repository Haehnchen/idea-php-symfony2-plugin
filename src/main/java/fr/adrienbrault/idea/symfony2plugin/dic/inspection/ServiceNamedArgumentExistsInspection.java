package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceNamedArgumentExistsInspection extends LocalInspectionTool {
    public static final String INSPECTION_MESSAGE = "Symfony: named argument does not exists";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (YamlElementPatternHelper.getNamedArgumentPattern().accepts(element)) {
                    if (isSupportedDefinition(element) && ServiceContainerUtil.hasMissingYamlNamedArgumentForInspection(element, new ContainerCollectionResolver.LazyServiceCollector(element.getProject()))) {
                        holder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    }
                }

                super.visitElement(element);
            }
        };
    }

    private boolean isSupportedDefinition(@NotNull PsiElement element) {
        PsiElement context = element.getContext();

        if (context instanceof YAMLKeyValue) {
            // arguments: ['$foobar': '@foo']
            PsiElement yamlMapping = context.getParent();
            if (yamlMapping instanceof YAMLMapping) {
                PsiElement yamlKeyValue = yamlMapping.getParent();
                if (yamlKeyValue instanceof YAMLKeyValue) {
                    YAMLMapping parentMapping = ((YAMLKeyValue) yamlKeyValue).getParentMapping();
                    if (parentMapping != null) {
                        return parentMapping.getKeyValueByKey("factory") == null;
                    }
                }
            }
        }

        return true;
    }
}
