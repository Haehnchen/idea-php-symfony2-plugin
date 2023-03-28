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
                    visitRoot(xmlAttributeValue, holder, "services", "service", "id", "Symfony: Duplicate Key");
                }

                super.visitElement(element);
            }
        };
    }

    public static void visitRoot(@NotNull XmlAttributeValue xmlAttributeValue, @NotNull ProblemsHolder holder, @NotNull  String root, @NotNull String child, @NotNull String tagName, @NotNull String message) {
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
                        holder.registerProblem(xmlAttributeValue, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        break;
                    }
                }
            }
        }
    }
}
