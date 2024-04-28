package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.InspectionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerMethodInspection {
    public static class ControllerMethodInspectionYaml extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new PsiElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (YamlElementPatternHelper.getSingleLineScalarKey("_controller", "controller").accepts(element)) {
                        String text = PsiElementUtils.trimQuote(element.getText());
                        if (StringUtils.isNotBlank(text)) {
                            InspectionUtil.inspectController(element, text, holder, new YamlLazyRouteName(element));
                        }
                    }

                    super.visitElement(element);
                }
            };
        }
    }

    public static class ControllerMethodInspectionXml extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new PsiElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if(XmlHelper.getRouteControllerPattern().accepts(element)) {
                        String text = PsiElementUtils.trimQuote(element.getText());
                        if(StringUtils.isNotBlank(text)) {
                            InspectionUtil.inspectController(element, text, holder, new XmlLazyRouteName(element));
                        }
                    }

                    super.visitElement(element);
                }
            };
        }
    }

    private record YamlLazyRouteName(@NotNull PsiElement psiElement) implements InspectionUtil.LazyControllerNameResolve {
        @Nullable
        @Override
        public String getRouteName() {
            YAMLKeyValue defaultKeyValue = PsiTreeUtil.getParentOfType(this.psiElement.getParent(), YAMLKeyValue.class);
            if (defaultKeyValue == null) {
                return null;
            }

            YAMLKeyValue def = PsiTreeUtil.getParentOfType(defaultKeyValue, YAMLKeyValue.class);
            if (def == null) {
                return null;
            }

            return YamlHelper.getYamlKeyName(def);
        }
    }

    private record XmlLazyRouteName(@NotNull PsiElement psiElement) implements InspectionUtil.LazyControllerNameResolve {
        @Nullable
        @Override
        public String getRouteName() {
            XmlTag defaultTag = PsiTreeUtil.getParentOfType(this.psiElement, XmlTag.class);
            if (defaultTag != null) {
                XmlTag routeTag = PsiTreeUtil.getParentOfType(defaultTag, XmlTag.class);
                if (routeTag != null) {
                    XmlAttribute id = routeTag.getAttribute("id");
                    if (id != null) {
                        return id.getValue();
                    }
                }
            }

            return null;
        }
    }
}
