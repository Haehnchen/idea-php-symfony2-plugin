package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.type;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.doctrine.DoctrineStaticTypeLookupBuilder;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.type.util.DoctrineMetadataTypeUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineTypeGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {

        // <field type="string" />
        registrar.register(
            XmlPatterns.psiElement().withParent(DoctrineMetadataPattern.getFieldType()), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
                return new MyTypeGotoCompletionProvider(psiElement) {
                    @Nullable
                    @Override
                    protected String getElementText(@NotNull PsiElement element) {
                        return GotoCompletionUtil.getXmlAttributeValue(element);
                    }
                };
            }
        });

        // yml files
        registrar.register(
            YamlElementPatternHelper.getOrmSingleLineScalarKey("type"), new GotoCompletionContributor() {
                @Nullable
                @Override
                public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
                    return new MyTypeGotoCompletionProvider(psiElement) {
                        @Nullable
                        @Override
                        protected String getElementText(@NotNull PsiElement element) {
                            String text = PsiElementUtils.trimQuote(element.getText());
                            if(StringUtils.isBlank(text)) {
                                return null;
                            }
                            return text;
                        }
                    };
                }
            }
        );

        // <field name="id" />
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlPatterns.or(DoctrineMetadataPattern.getFieldName(), DoctrineMetadataPattern.getFieldNameRelation())), new GotoCompletionContributor() {
                @Nullable
                @Override
                public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
                    return new MyFieldNameGotoCompletionProvider(psiElement) {
                        @Nullable
                        @Override
                        protected String getElementText(@NotNull PsiElement element) {
                            return GotoCompletionUtil.getXmlAttributeValue(element);
                        }
                    };
                }
            }
        );
    }

    private abstract static class MyTypeGotoCompletionProvider extends GotoCompletionProvider {

        public MyTypeGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            final Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

            Collection<String> typeClassesByScopeWithAllFallback = DoctrineMetadataTypeUtil.getTypeClassesByScopeWithAllFallback(getElement());
            DoctrineMetadataTypeUtil.visitType(getProject(), typeClassesByScopeWithAllFallback, new Processor<Pair<PhpClass, String>>() {
                @Override
                public boolean process(Pair<PhpClass, String> pair) {
                    lookupElements.add(
                        LookupElementBuilder.create(pair.getSecond())
                            .withIcon(Symfony2Icons.DOCTRINE)
                            .withTypeText(pair.getSecond(), true)
                    );
                    return true;
                }
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

            final Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

            DoctrineMetadataTypeUtil.visitType(getProject(), DoctrineMetadataTypeUtil.getTypeClassesByScopeWithAllFallback(element), new Processor<Pair<PhpClass, String>>() {
                @Override
                public boolean process(Pair<PhpClass, String> pair) {
                    if (pair.getSecond().equalsIgnoreCase(value)) {
                        psiElements.add(pair.getFirst());
                    }
                    return true;
                }
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

            Set<String> unique = new HashSet<String>();

            Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();
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

            final Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

            for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(getProject(), modelNameInScope)) {
                psiElements.addAll(ContainerUtil.filter(phpClass.getFields(), new Condition<Field>() {
                    @Override
                    public boolean value(Field field) {
                        return !field.isConstant() && elementText.equals(field.getName());
                    }
                }));
            }

            return psiElements;
        }

        @Nullable
        abstract protected String getElementText(@NotNull PsiElement element);
    }
}
