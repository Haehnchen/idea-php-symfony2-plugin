package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerSettingDeprecatedInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if(element instanceof XmlAttribute) {
                    registerXmlAttributeProblem(holder, (XmlAttribute) element);
                } else if(element instanceof YAMLKeyValue) {
                    registerYmlRoutePatternProblem(holder, (YAMLKeyValue) element);
                }

                super.visitElement(element);
            }
        };
    }

    private void registerYmlRoutePatternProblem(@NotNull ProblemsHolder holder, @NotNull YAMLKeyValue element) {
        String s = PsiElementUtils.trimQuote(element.getKeyText());
        if(("factory_class".equals(s) || "factory_method".equals(s) || "factory_service".equals(s)) && YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(element)) {
            // services:
            //   foo:
            //      factory_*:
            registerProblem(holder, element.getKey());
        }
    }

    private void registerXmlAttributeProblem(@NotNull ProblemsHolder holder, @NotNull XmlAttribute xmlAttribute) {
        String name = xmlAttribute.getName();
        if(!("factory-class".equals(name) || "factory-method".equals(name) || "factory-service".equals(name))) {
            return;
        }

        XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttribute, XmlTag.class, "service");
        if(xmlTagRoute != null) {
            registerProblem(holder, xmlAttribute.getFirstChild());
        }
    }

    private void registerProblem(@NotNull ProblemsHolder holder, @Nullable PsiElement target) {
        if(target == null) {
            return;
        }

        holder.registerProblem(target, "Symfony: this factory pattern is deprecated use 'factory' instead", ProblemHighlightType.LIKE_DEPRECATED);
    }
}
