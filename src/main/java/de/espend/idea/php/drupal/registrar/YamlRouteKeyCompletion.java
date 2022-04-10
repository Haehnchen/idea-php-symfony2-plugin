package de.espend.idea.php.drupal.registrar;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import de.espend.idea.php.drupal.DrupalIcons;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlRouteKeyCompletion implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        PsiElementPattern.Capture<PsiElement> filePattern = PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile().withName(PlatformPatterns.string().endsWith(".routing.yml")));

        registrar.register(PlatformPatterns.and(YamlElementPatternHelper.getParentKeyName("defaults"), filePattern), psiElement -> {
            if(!DrupalProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new DefaultRoutes(psiElement);
        });

        registrar.register(PlatformPatterns.and(YamlElementPatternHelper.getParentKeyName("requirements"), filePattern), psiElement -> {
            if(!DrupalProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new EntityAccessRoutes(psiElement);
        });
    }

    private static class DefaultRoutes extends GotoCompletionProvider {
        DefaultRoutes(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<>();
            for (String s : new String[]{"_entity_form", "_title_callback", "op", "_entity_access", "_entity_list", "_controller"}) {
                lookupElements.add(LookupElementBuilder.create(s).withIcon(DrupalIcons.DRUPAL).withTypeText("Routing", true));
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            return Collections.emptyList();
        }
    }

    private static class EntityAccessRoutes extends GotoCompletionProvider {
        EntityAccessRoutes(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<>();
            for (String s : new String[]{"_entity_access"}) {
                lookupElements.add(LookupElementBuilder.create(s).withIcon(DrupalIcons.DRUPAL).withTypeText("Routing", true));
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            return Collections.emptyList();
        }
    }
}
