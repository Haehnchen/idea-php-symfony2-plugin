package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TaggedExtendsInterfaceClassInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile psiFile) {
                if(psiFile instanceof YAMLFile) {
                    psiFile.acceptChildren(new YmlClassElementWalkingVisitor(holder, new ContainerCollectionResolver.LazyServiceCollector(holder.getProject())));
                } else if(psiFile instanceof XmlFile) {
                    psiFile.acceptChildren(new XmlClassElementWalkingVisitor(holder, new ContainerCollectionResolver.LazyServiceCollector(holder.getProject())));
                }
            }
        };
    }

    private class XmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;
        private final ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        public XmlClassElementWalkingVisitor(ProblemsHolder holder, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
            this.holder = holder;
            this.lazyServiceCollector = lazyServiceCollector;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            String className = getClassNameFromServiceDefinition(element);
            if (className != null) {
                XmlTag parentOfType = PsiTreeUtil.getParentOfType(element, XmlTag.class);
                if(parentOfType != null) {
                    // attach problems to string value only
                    PsiElement[] psiElements = element.getChildren();
                    if (psiElements.length > 2) {
                        registerTaggedProblems(psiElements[1], FormUtil.getTags(parentOfType), className, holder, this.lazyServiceCollector);
                    }
                }
            }

            super.visitElement(element);
        }
    }

    private class YmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;
        private final ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        public YmlClassElementWalkingVisitor(ProblemsHolder holder, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
            this.holder = holder;
            this.lazyServiceCollector = lazyServiceCollector;
        }

        @Override
        public void visitElement(@NotNull PsiElement psiElement) {
            if(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(psiElement)) {

                // class: '\Foo'
                String text = PsiElementUtils.trimQuote(psiElement.getText());
                if(StringUtils.isBlank(text)) {
                    super.visitElement(psiElement);
                    return;
                }

                PsiElement yamlScalar = psiElement.getParent();
                if(!(yamlScalar instanceof YAMLScalar)) {
                    super.visitElement(psiElement);
                    return;
                }

                PsiElement classKey = yamlScalar.getParent();
                if(classKey instanceof YAMLKeyValue) {
                    PsiElement yamlCompoundValue = classKey.getParent();
                    if(yamlCompoundValue instanceof YAMLCompoundValue) {
                        PsiElement serviceKeyValue = yamlCompoundValue.getParent();
                        if(serviceKeyValue instanceof YAMLKeyValue) {
                            Set<String> tags = YamlHelper.collectServiceTags((YAMLKeyValue) serviceKeyValue);
                            if(tags.size() > 0) {
                                registerTaggedProblems(psiElement, tags, text, holder, this.lazyServiceCollector);
                            }
                        }
                    }
                }
            } else if (psiElement.getNode().getElementType() == YAMLTokenTypes.SCALAR_KEY && YamlElementPatternHelper.getServiceIdKeyValuePattern().accepts(psiElement.getParent())) {
                // Foobar\Foo: ~
                String text = PsiElementUtils.getText(psiElement);
                if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\")) {
                    PsiElement yamlKeyValue = psiElement.getParent();
                    if (yamlKeyValue instanceof YAMLKeyValue && YamlHelper.getYamlKeyValue((YAMLKeyValue) yamlKeyValue, "resource") == null && YamlHelper.getYamlKeyValue((YAMLKeyValue) yamlKeyValue, "exclude") == null) {
                        Set<String> tags = YamlHelper.collectServiceTags((YAMLKeyValue) yamlKeyValue);
                        if(tags.size() > 0) {
                            registerTaggedProblems(psiElement, tags, text, holder, this.lazyServiceCollector);
                        }
                    }
                }
            }

            super.visitElement(psiElement);
        }
    }

    private void registerTaggedProblems(@NotNull PsiElement source, @NotNull Set<String> tags, @NotNull String serviceClass, @NotNull ProblemsHolder holder, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        if (tags.size() == 0) {
            return;
        }

        PhpClass phpClass = null;

        for (String tag : tags) {
            String missingTagInstance = null;

            for (String expectedClass : ServiceUtil.TAG_INTERFACES.getOrDefault(tag, new String[] {})) {
                // load PhpClass only if we need it, on error exit
                if(phpClass == null) {
                    phpClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceClass, lazyServiceCollector);
                    if(phpClass == null) {
                        return;
                    }
                }

                // skip unknown classes
                if (PhpElementsUtil.getClassesInterface(phpClass.getProject(), expectedClass).isEmpty()) {
                    continue;
                }

                // check interfaces
                if(!PhpElementsUtil.isInstanceOf(phpClass, expectedClass)) {
                    missingTagInstance = expectedClass;
                    continue;
                }

                missingTagInstance = null;
                break;
            }

            // check interfaces
            if (missingTagInstance != null) {
                holder.registerProblem(source, String.format("Class needs to implement '%s' for tag '%s'", StringUtils.stripStart(missingTagInstance, "\\"), tag), ProblemHighlightType.WEAK_WARNING);
            }
        }
    }

    /**
     * <service class="Foo\\Bar" id="required_attribute">
     * <service id="Foo\\Bar" />
     */
    @Nullable
    private static String getClassNameFromServiceDefinition(@NotNull PsiElement element) {
        if (XmlHelper.getServiceClassAttributeWithIdPattern().accepts(element)) {
            // <service class="Foo\\Bar" id="required_attribute">
            String text = PsiElementUtils.trimQuote(element.getText());
            if (StringUtils.isNotBlank(text)) {
                return text;
            }
        } else if (XmlHelper.getServiceIdAttributePattern().accepts(element)) {
            // <service id="Foo\\Bar" />
            String text = PsiElementUtils.trimQuote(element.getText());
            if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\")) {
                return text;
            }
        }

        return null;
    }
}
