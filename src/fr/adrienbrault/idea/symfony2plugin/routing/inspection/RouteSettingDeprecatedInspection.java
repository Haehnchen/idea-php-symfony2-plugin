package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Condition;
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
public class RouteSettingDeprecatedInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(element instanceof XmlAttributeValue) {
                    registerAttributeRequirementProblem(holder, (XmlAttributeValue) element, "_method");
                    registerAttributeRequirementProblem(holder, (XmlAttributeValue) element, "_scheme");
                } else if(element instanceof XmlAttribute) {
                    registerRoutePatternProblem(holder, (XmlAttribute) element);
                } else if(element instanceof YAMLKeyValue) {
                    registerYmlRoutePatternProblem(holder, (YAMLKeyValue) element);
                }

                super.visitElement(element);
            }
        };
    }

    private void registerYmlRoutePatternProblem(@NotNull ProblemsHolder holder, @NotNull YAMLKeyValue element) {
        String s = PsiElementUtils.trimQuote(element.getKeyText());
        if("pattern".equals(s) && YamlHelper.isRoutingFile(element.getContainingFile())) {
            // pattern: foo
            holder.registerProblem(element.getKey(), "Pattern is deprecated; use path instead", ProblemHighlightType.LIKE_DEPRECATED);

        } else if(("_method".equals(s) || "_scheme".equals(s)) && YamlHelper.isRoutingFile(element.getContainingFile())) {
            // requirements: { _method: 'foo', '_scheme': 'foo' }
            YAMLKeyValue parentOfType = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
            if(parentOfType != null && "requirements".equals(parentOfType.getKeyText())) {
                holder.registerProblem(element.getKey(), String.format("The '%s' requirement is deprecated", s), ProblemHighlightType.LIKE_DEPRECATED);
            }
        }
    }

    private void registerRoutePatternProblem(@NotNull ProblemsHolder holder, @NotNull XmlAttribute xmlAttribute) {
        if(!"pattern".equals(xmlAttribute.getName())) {
            return;
        }

        XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttribute, XmlTag.class, "route");
        if(xmlTagRoute != null) {
            holder.registerProblem(xmlAttribute, "Pattern is deprecated; use path instead", ProblemHighlightType.LIKE_DEPRECATED);
        }
    }

    private void registerAttributeRequirementProblem(@NotNull ProblemsHolder holder, @NotNull XmlAttributeValue xmlAttributeValue, @NotNull final String requirementAttribute) {
        if(!xmlAttributeValue.getValue().equals(requirementAttribute)) {
            return;
        }

        XmlAttribute xmlAttributeKey = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttributeValue, XmlAttribute.class, "key");
        if(xmlAttributeKey != null) {
            XmlTag xmlTagDefault = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttributeKey, XmlTag.class, "requirement");
            if(xmlTagDefault != null) {
                XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlTagDefault, XmlTag.class, "route");
                if(xmlTagRoute != null) {
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
    private PsiElement findAttributeValueToken(@NotNull XmlAttributeValue xmlAttributeValue, @NotNull final String attributeText) {
        return ContainerUtil.find(xmlAttributeValue.getChildren(), new Condition<PsiElement>() {
            @Override
            public boolean value(PsiElement psiElement) {
                return psiElement instanceof XmlToken && attributeText.equals(psiElement.getText());
            }
        });
    }
}
