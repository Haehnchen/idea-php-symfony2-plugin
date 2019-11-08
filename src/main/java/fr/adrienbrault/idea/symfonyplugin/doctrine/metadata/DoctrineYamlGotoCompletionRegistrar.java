package fr.adrienbrault.idea.symfonyplugin.doctrine.metadata;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineYamlGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        registrar.register(PlatformPatterns.or(
            YamlElementPatternHelper.getOrmSingleLineScalarKey("repositoryClass")
        ), ClassGotoCompletionProvider::new);
    }

    private static class ClassGotoCompletionProvider extends GotoCompletionProvider {
        private ClassGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return DoctrineMetadataUtil.getObjectRepositoryLookupElements(getProject());
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String psiText = PsiElementUtils.getText(element);
            if(StringUtils.isBlank(psiText)) {
                return Collections.emptyList();
            }

            return new ArrayList<>(DoctrineMetadataUtil.getClassInsideScope(element, psiText));
        }
    }
}
