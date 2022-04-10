package de.espend.idea.php.drupal.registrar;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import de.espend.idea.php.drupal.DrupalIcons;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.index.MenuIndex;
import de.espend.idea.php.drupal.registrar.utils.YamlRegistrarUtil;
import de.espend.idea.php.drupal.utils.IndexUtil;
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
public class YamlMenuGotoCompletion implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        PsiElementPattern.Capture<PsiElement> menuPattern = PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile().withName(PlatformPatterns.string().endsWith(".menu.yml")));

        registrar.register(PlatformPatterns.and(YamlElementPatternHelper.getSingleLineScalarKey("parent"), menuPattern), psiElement -> {
            if(!DrupalProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new ParentMenu(psiElement);
        });


        registrar.register(PlatformPatterns.and(YamlElementPatternHelper.getWithFirstRootKey(), menuPattern), psiElement -> {
            if(!DrupalProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new MenuKeys(psiElement);
        });
    }

    private static class ParentMenu extends GotoCompletionProvider {
        ParentMenu(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return IndexUtil.getIndexedKeyLookup(getProject(), MenuIndex.KEY);
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            String text = YamlRegistrarUtil.getYamlScalarKey(psiElement);
            if(text == null) {
                return Collections.emptyList();
            }

            return new ArrayList<>(IndexUtil.getMenuForId(getProject(), text));
        }
    }

    private static class MenuKeys extends GotoCompletionProvider {
        MenuKeys(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            Collection<LookupElement> lookupElements = new ArrayList<>();
            for (String s : new String[]{"title", "route_name", "enabled", "parent", "description", "weight"}) {
                lookupElements.add(LookupElementBuilder.create(s).withIcon(DrupalIcons.DRUPAL).withTypeText("Menu", true));
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
