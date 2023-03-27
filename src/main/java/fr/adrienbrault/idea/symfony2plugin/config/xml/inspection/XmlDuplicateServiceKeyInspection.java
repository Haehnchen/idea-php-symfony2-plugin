package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlDuplicateServiceKeyInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof XmlAttributeValue xmlAttributeValue) {
                    visitRoot(xmlAttributeValue, holder, "services", "service", "id");
                }

                super.visitElement(element);
            }
        };
    }

    protected void visitRoot(@NotNull XmlAttributeValue xmlAttributeValue, @NotNull ProblemsHolder holder, String root, String child, String tagName) {
        String value = null;

        if (xmlAttributeValue.getParent() instanceof XmlAttribute xmlAttribute && tagName.equals(xmlAttribute.getName())) {
            XmlTag xmlTag = xmlAttribute.getParent();
            if (xmlTag != null && child.equals(xmlTag.getName()) && xmlTag.getParent() instanceof XmlTag rootContextXmlTag && root.equals(rootContextXmlTag.getName())) {
                int found = 0;
                for (XmlTag parameters : rootContextXmlTag.findSubTags(child)) {
                    String key = parameters.getAttributeValue(tagName);

                    // lazy value resolve
                    if (value == null) {
                        value = xmlAttributeValue.getValue();
                    }

                    if (value.equals(key)) {
                        found++;
                    }

                    if (found == 2) {
                        holder.registerProblem(xmlAttributeValue, "Symfony: Duplicate Key", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        break;
                    }
                }
            }
        }
    }
}
