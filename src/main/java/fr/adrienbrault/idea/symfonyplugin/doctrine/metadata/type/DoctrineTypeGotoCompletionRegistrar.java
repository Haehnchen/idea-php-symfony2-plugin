package fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.type;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfonyplugin.config.doctrine.DoctrineStaticTypeLookupBuilder;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.type.util.DoctrineMetadataTypeUtil;
import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineTypeGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {

        // <field type="string" />
        registrar.register(
            XmlPatterns.psiElement().withParent(DoctrineMetadataPattern.getFieldType()), psiElement -> new MyTypeGotoCompletionProvider(psiElement) {
                @Nullable
                @Override
                protected String getElementText(@NotNull PsiElement element) {
                    return GotoCompletionUtil.getXmlAttributeValue(element);
                }
            });

        // yml files
        registrar.register(
            YamlElementPatternHelper.getOrmSingleLineScalarKey("type"), psiElement -> new MyTypeGotoCompletionProvider(psiElement) {
                @Nullable
                @Override
                protected String getElementText(@NotNull PsiElement element) {
                    String text = PsiElementUtils.trimQuote(element.getText());
                    if(StringUtils.isBlank(text)) {
                        return null;
                    }
                    return text;
                }
            }
        );

        // <field name="id" />
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlPatterns.or(DoctrineMetadataPattern.getFieldName(), DoctrineMetadataPattern.getFieldNameRelation())), psiElement -> new MyFieldNameGotoCompletionProvider(psiElement) {
                @Nullable
                @Override
                protected String getElementText(@NotNull PsiElement element) {
                    return GotoCompletionUtil.getXmlAttributeValue(element);
                }
            }
        );

        // fields:
        //   i<caret>d: []
        registrar.register(
            DoctrineMetadataPattern.getYamlFieldName(), MyYamlFieldNameGotoCompletionProvider::new
        );
    }

    private abstract static class MyTypeGotoCompletionProvider extends GotoCompletionProvider {

        public MyTypeGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            final Collection<LookupElement> lookupElements = new ArrayList<>();

            Collection<String> typeClassesByScopeWithAllFallback = DoctrineMetadataTypeUtil.getTypeClassesByScopeWithAllFallback(getElement());
            DoctrineMetadataTypeUtil.visitType(getProject(), typeClassesByScopeWithAllFallback, pair -> {
                lookupElements.add(
                    LookupElementBuilder.create(pair.getSecond())
                        .withIcon(Symfony2Icons.DOCTRINE)
                        .withTypeText(pair.getSecond(), true)
                );
                return true;
            });

            if(typeClassesByScopeWithAllFallback.contains(DoctrineMetadataTypeUtil.DBAL_TYPE)) {
                DoctrineStaticTypeLookupBuilder.fillOrmLookupElementsWithStatic(lookupElements);
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            final String value = getElementText(element);
            if(value == null) {
                return Collections.emptyList();
            }

            final Collection<PsiElement> psiElements = new ArrayList<>();

            DoctrineMetadataTypeUtil.visitType(getProject(), DoctrineMetadataTypeUtil.getTypeClassesByScopeWithAllFallback(element), pair -> {
                if (pair.getSecond().equalsIgnoreCase(value)) {
                    psiElements.add(pair.getFirst());
                }
                return true;
            });

            return psiElements;
        }

        @Nullable
        abstract protected String getElementText(@NotNull PsiElement element);
    }

    private abstract static class MyFieldNameGotoCompletionProvider extends GotoCompletionProvider {

        public MyFieldNameGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            String modelNameInScope = DoctrineMetadataUtil.findModelNameInScope(getElement());
            if(modelNameInScope == null) {
                return Collections.emptyList();
            }

            Set<String> unique = new HashSet<>();

            Collection<LookupElement> lookupElements = new ArrayList<>();
            for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(getProject(), modelNameInScope)) {
                for (Field field : phpClass.getFields()) {
                    if(field.isConstant() || unique.contains(field.getName())) {
                        continue;
                    }

                    String name = field.getName();
                    unique.add(name);
                    lookupElements.add(LookupElementBuilder.create(name).withIcon(field.getIcon()).withTypeText(phpClass.getPresentableFQN(), true));
                }
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {

            final String elementText = getElementText(element);
            if(elementText == null) {
                return Collections.emptyList();
            }

            String modelNameInScope = DoctrineMetadataUtil.findModelNameInScope(getElement());
            if(modelNameInScope == null) {
                return Collections.emptyList();
            }

            final Collection<PsiElement> psiElements = new ArrayList<>();

            for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(getProject(), modelNameInScope)) {
                psiElements.addAll(ContainerUtil.filter(phpClass.getFields(), field ->
                    !field.isConstant() && elementText.equals(field.getName()))
                );
            }

            return psiElements;
        }

        @Nullable
        abstract protected String getElementText(@NotNull PsiElement element);
    }

    private static class MyYamlFieldNameGotoCompletionProvider extends MyFieldNameGotoCompletionProvider {

        public MyYamlFieldNameGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        public Collection<LookupElement> getLookupElements() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        protected String getElementText(@NotNull PsiElement element) {
            PsiElement parent = element.getParent();
            if(!(parent instanceof YAMLKeyValue)) {
                return null;
            }

            String keyText = ((YAMLKeyValue) parent).getKeyText();
            if(StringUtils.isBlank(keyText)) {
                return null;
            }

            return keyText;
        }
    }
}
