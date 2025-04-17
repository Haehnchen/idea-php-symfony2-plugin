package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.dict.PhpDocCommentAnnotation;
import de.espend.idea.php.annotation.dict.PhpDocTagAnnotation;
import de.espend.idea.php.annotation.pattern.AnnotationPattern;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlDuplicateServiceKeyInspection;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DuplicateLocalRouteInspection extends LocalInspectionTool {
    private static final String MESSAGE = "Symfony: Duplicate route name";

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        public MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (element instanceof YAMLKeyValue yamlKeyValue && element.getLanguage() == YAMLLanguage.INSTANCE) {
                visitYaml(yamlKeyValue);
            } else if(element instanceof StringLiteralExpression && element.getLanguage() == PhpLanguage.INSTANCE) {
                visitPhp((StringLiteralExpression) element);
            } else if(element instanceof XmlAttributeValue xmlAttributeValue) {
                XmlDuplicateServiceKeyInspection.visitRoot(xmlAttributeValue, holder, "routes", "route", "id", MESSAGE);
            }

            super.visitElement(element);
        }

        private void visitYaml(@NotNull YAMLKeyValue yamlKeyValue) {
            if (YamlHelper.isRoutingFile(yamlKeyValue.getContainingFile()) && yamlKeyValue.getParent() instanceof YAMLMapping yamlMapping && yamlMapping.getParent() instanceof YAMLDocument) {
                String keyText1 = null;

                int found = 0;
                for (YAMLKeyValue keyValue : yamlMapping.getKeyValues()) {
                    String keyText = keyValue.getKeyText();

                    // lazy
                    if (keyText1 == null) {
                        keyText1 = yamlKeyValue.getKeyText();
                    }

                    if (keyText1.equals(keyText)) {
                        found++;
                    }

                    if (found == 2) {
                        final PsiElement keyElement = yamlKeyValue.getKey();
                        assert keyElement != null;
                        holder.registerProblem(keyElement, "Symfony: Duplicate route name", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);

                        break;
                    }
                }
            }
        }

        private void visitPhp(@NotNull StringLiteralExpression element) {
            if (PhpElementsUtil.isAttributeNamedArgumentString(element, "\\Symfony\\Component\\Routing\\Annotation\\Route", "name") || PhpElementsUtil.isAttributeNamedArgumentString(element, "\\Symfony\\Component\\Routing\\Attribute\\Route", "name")) {
                PhpAttribute parentOfType = PsiTreeUtil.getParentOfType(element, PhpAttribute.class);
                if (parentOfType.getOwner() instanceof Method method && method.getAccess().isPublic() && method.getContainingClass() != null) {
                    int found = 0;
                    String contents = element.getContents();

                    for (Method ownMethod : method.getContainingClass().getOwnMethods()) {
                        Collection<PhpAttribute> attributes = new ArrayList<>(ownMethod.getAttributes("\\Symfony\\Component\\Routing\\Annotation\\Route"));
                        attributes.addAll(ownMethod.getAttributes("\\Symfony\\Component\\Routing\\Attribute\\Route"));

                        for (PhpAttribute attribute : attributes) {
                            String name = PhpElementsUtil.getAttributeArgumentStringByName(attribute, "name");
                            if (contents.equals(name)) {
                                found++;
                            }

                            if (found == 2) {
                                holder.registerProblem(element, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                                return;
                            }
                        }
                    }
                }
            }

            if (AnnotationPattern.getPropertyIdentifierValue("name").accepts(element)) {
                PhpDocTag phpDocTag = PsiTreeUtil.getParentOfType(element, PhpDocTag.class);
                if (phpDocTag != null) {
                    PhpClass phpClass = AnnotationUtil.getAnnotationReference(phpDocTag);
                    if (phpClass != null && RouteHelper.isRouteClassAnnotation(phpClass.getFQN())) {
                        PhpDocComment phpDocComment = PsiTreeUtil.getParentOfType(element, PhpDocComment.class);
                        if (phpDocComment.getNextPsiSibling() instanceof Method method && method.getAccess().isPublic() && method.getContainingClass() != null) {
                            int found = 0;
                            String contents = element.getContents();

                            for (Method ownMethod : method.getContainingClass().getOwnMethods()) {
                                PhpDocCommentAnnotation phpClassContainer = AnnotationUtil.getPhpDocCommentAnnotationContainer(ownMethod.getDocComment());
                                if(phpClassContainer != null) {
                                    PhpDocTagAnnotation firstPhpDocBlock = phpClassContainer.getFirstPhpDocBlock(RouteHelper.ROUTE_ANNOTATIONS);
                                    if(firstPhpDocBlock != null) {
                                        String name = firstPhpDocBlock.getPropertyValue("name");
                                        if (contents.equals(name)) {
                                            found++;
                                        }

                                        if (found == 2) {
                                            holder.registerProblem(element, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
