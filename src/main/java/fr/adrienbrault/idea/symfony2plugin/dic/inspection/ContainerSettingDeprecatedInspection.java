package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.patterns.ElementPattern;
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
public class ContainerSettingDeprecatedInspection {

    public static class ContainerSettingDeprecatedInspectionYaml extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new MyYamlPsiElementVisitor(holder);
        }

        private static class MyYamlPsiElementVisitor extends PsiElementVisitor {
            @NotNull private final ProblemsHolder holder;
            private ElementPattern<PsiElement> insideServiceKeyPattern;

            MyYamlPsiElementVisitor(@NotNull ProblemsHolder holder) {
                this.holder = holder;
            }

            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof YAMLKeyValue) {
                    registerYmlRoutePatternProblem(holder, (YAMLKeyValue) element, getInsideServiceKeyPattern());
                }

                super.visitElement(element);
            }

            private ElementPattern<PsiElement> getInsideServiceKeyPattern() {
                return insideServiceKeyPattern != null ? insideServiceKeyPattern : (insideServiceKeyPattern = YamlElementPatternHelper.getInsideServiceKeyPattern());
            }
        }
    }

    public static class ContainerSettingDeprecatedInspectionXml extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new PsiElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (element instanceof XmlAttribute) {
                        registerXmlAttributeProblem(holder, (XmlAttribute) element);
                    }

                    super.visitElement(element);
                }
            };
        }
    }

    private static void registerYmlRoutePatternProblem(@NotNull ProblemsHolder holder, @NotNull YAMLKeyValue element, @NotNull ElementPattern<PsiElement> insideServiceKeyPattern) {
        String s = PsiElementUtils.trimQuote(element.getKeyText());
        if (("factory_class".equals(s) || "factory_method".equals(s) || "factory_service".equals(s)) && insideServiceKeyPattern.accepts(element)) {
            // services:
            //   foo:
            //      factory_*:
            registerProblem(holder, element.getKey());
        }
    }

    private static void registerXmlAttributeProblem(@NotNull ProblemsHolder holder, @NotNull XmlAttribute xmlAttribute) {
        String name = xmlAttribute.getName();
        if (!("factory-class".equals(name) || "factory-method".equals(name) || "factory-service".equals(name))) {
            return;
        }

        XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttribute, XmlTag.class, "service");
        if (xmlTagRoute != null) {
            registerProblem(holder, xmlAttribute.getFirstChild());
        }
    }

    private static void registerProblem(@NotNull ProblemsHolder holder, @Nullable PsiElement target) {
        if (target == null) {
            return;
        }

        holder.registerProblem(target, "Symfony: this factory pattern is deprecated use 'factory' instead", ProblemHighlightType.LIKE_DEPRECATED);
    }
}
