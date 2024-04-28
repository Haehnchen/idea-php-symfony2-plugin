package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteSettingDeprecatedInspection {
    public static class RouteSettingDeprecatedInspectionYaml extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new PsiElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (element instanceof YAMLKeyValue yamlKeyValue) {
                        registerYmlRoutePatternProblem(holder, yamlKeyValue);
                    }

                    super.visitElement(element);
                }
            };
        }
    }

    public static class RouteSettingDeprecatedInspectionXml extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new PsiElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (element instanceof XmlAttributeValue xmlAttributeValue) {
                        registerAttributeRequirementProblem(holder, xmlAttributeValue, "_method");
                        registerAttributeRequirementProblem(holder, xmlAttributeValue, "_scheme");
                    } else if (element instanceof XmlAttribute xmlAttribute) {
                        registerRoutePatternProblem(holder, xmlAttribute);
                    }

                    super.visitElement(element);
                }
            };
        }
    }

    private static void registerYmlRoutePatternProblem(@NotNull ProblemsHolder holder, @NotNull YAMLKeyValue element) {
        PsiElement key = element.getKey();
        if (key == null) {
            return;
        }

        String s = PsiElementUtils.trimQuote(element.getKeyText());
        if("pattern".equals(s) && YamlHelper.isRoutingFile(element.getContainingFile())) {
            // pattern: foo
            holder.registerProblem(key, "Pattern is deprecated; use path instead", ProblemHighlightType.LIKE_DEPRECATED);

        } else if(("_method".equals(s) || "_scheme".equals(s)) && YamlHelper.isRoutingFile(element.getContainingFile())) {
            // requirements: { _method: 'foo', '_scheme': 'foo' }
            YAMLKeyValue parentOfType = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
            if(parentOfType != null && "requirements".equals(parentOfType.getKeyText())) {
                holder.registerProblem(key, String.format("The '%s' requirement is deprecated", s), ProblemHighlightType.LIKE_DEPRECATED);
            }
        }
    }

    private static void registerRoutePatternProblem(@NotNull ProblemsHolder holder, @NotNull XmlAttribute xmlAttribute) {
        if (!"pattern".equals(xmlAttribute.getName())) {
            return;
        }

        XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttribute, XmlTag.class, "route");
        if (xmlTagRoute != null && xmlAttribute.getFirstChild() != null) {
            holder.registerProblem(xmlAttribute.getFirstChild(), "Pattern is deprecated; use path instead", ProblemHighlightType.LIKE_DEPRECATED);
        }
    }

    private static void registerAttributeRequirementProblem(@NotNull ProblemsHolder holder, @NotNull XmlAttributeValue xmlAttributeValue, @NotNull final String requirementAttribute) {
        if (!xmlAttributeValue.getValue().equals(requirementAttribute)) {
            return;
        }

        XmlAttribute xmlAttributeKey = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttributeValue, XmlAttribute.class, "key");
        if (xmlAttributeKey != null) {
            XmlTag xmlTagDefault = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttributeKey, XmlTag.class, "requirement");
            if (xmlTagDefault != null) {
                XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlTagDefault, XmlTag.class, "route");
                if (xmlTagRoute != null) {
                    // attach to attribute token only we dont want " or ' char included
                    PsiElement target = findAttributeValueToken(xmlAttributeValue, requirementAttribute);

                    holder.registerProblem(target != null ? target : xmlAttributeValue, String.format("The '%s' requirement is deprecated", requirementAttribute), ProblemHighlightType.LIKE_DEPRECATED);
                }
            }
        }
    }

    /**
     * Find child token which stores value
     *
     * XmlToken: "'"
     * XmlToken: "attributeText"
     * XmlToken: "'"
     */
    private static PsiElement findAttributeValueToken(@NotNull XmlAttributeValue xmlAttributeValue, @NotNull final String attributeText) {
        return ContainerUtil.find(xmlAttributeValue.getChildren(), psiElement ->
            psiElement instanceof XmlToken && attributeText.equals(psiElement.getText())
        );
    }
}
