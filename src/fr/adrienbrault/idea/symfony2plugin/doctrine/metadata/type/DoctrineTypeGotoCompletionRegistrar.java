package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.type;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
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
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineTypeGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {

        // xml files
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
            });
    }

    abstract static class MyTypeGotoCompletionProvider extends GotoCompletionProvider {

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
}
