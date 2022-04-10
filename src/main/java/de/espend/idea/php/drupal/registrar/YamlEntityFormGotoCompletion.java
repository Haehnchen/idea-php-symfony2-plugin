package de.espend.idea.php.drupal.registrar;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.index.ConfigEntityTypeAnnotationIndex;
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
public class YamlEntityFormGotoCompletion implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(YamlElementPatternHelper.getSingleLineScalarKey("_entity_form"), psiElement -> {
            if(!DrupalProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new MyGotoCompletionProvider(psiElement);
        });
    }

    private static class MyGotoCompletionProvider extends GotoCompletionProvider {
        public MyGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return IndexUtil.getIndexedKeyLookup(getProject(), ConfigEntityTypeAnnotationIndex.KEY);
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            String text = YamlRegistrarUtil.getYamlScalarKey(psiElement);
            if(text == null) {
                return Collections.emptyList();
            }

            return new ArrayList<>(IndexUtil.getFormClassForId(getProject(), text));
        }
    }
}
