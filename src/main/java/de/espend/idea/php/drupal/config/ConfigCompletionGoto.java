package de.espend.idea.php.drupal.config;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.index.ConfigSchemaIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigCompletionGoto implements GotoCompletionRegistrar {

    final private static MethodMatcher.CallToSignature[] CONFIG = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Drupal\\Core\\Config\\ConfigFactory", "get"),
    };

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), psiElement -> {

            if(!DrupalProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            PsiElement parent = psiElement.getParent();
            if(parent == null) {
                return null;
            }

            MethodMatcher.MethodMatchParameter methodMatchParameter = MethodMatcher.getMatchedSignatureWithDepth(parent, CONFIG);
            if(methodMatchParameter == null) {
                return null;
            }

            return new FormReferenceCompletionProvider(parent);

        });
    }

    private static class FormReferenceCompletionProvider extends GotoCompletionProvider {

        private FormReferenceCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            for(String config: SymfonyProcessors.createResult(getProject(), ConfigSchemaIndex.KEY)) {
                lookupElements.add(LookupElementBuilder.create(config).withIcon(Symfony2Icons.CONFIG_VALUE));
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {

            PsiElement element = psiElement.getParent();
            if(!(element instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            final String contents = ((StringLiteralExpression) element).getContents();
            if(StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            final Collection<PsiElement> psiElements = new ArrayList<>();
            FileBasedIndex.getInstance().getFilesWithKey(ConfigSchemaIndex.KEY, new HashSet<>(Collections.singletonList(contents)), virtualFile -> {

                PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(virtualFile);
                if(psiFile == null) {
                    return true;
                }

                YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
                if(yamlDocument == null) {
                    return true;
                }

                for(YAMLKeyValue yamlKeyValue: PsiTreeUtil.getChildrenOfTypeAsList(yamlDocument, YAMLKeyValue.class)) {
                    String keyText = PsiElementUtils.trimQuote(yamlKeyValue.getKeyText());
                    if(StringUtils.isNotBlank(keyText) && keyText.equals(contents)) {
                        psiElements.add(yamlKeyValue);
                    }
                }

                return true;
            }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(getProject()), YAMLFileType.YML));

            return psiElements;
        }
    }

}
