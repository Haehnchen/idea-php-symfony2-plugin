package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
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

        public ClassGotoCompletionProvider(PsiElement element) {
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

            Collection<PsiElement> classes = new ArrayList<>();
            for (PhpClass phpClass : DoctrineMetadataUtil.getClassInsideScope(element, psiText)) {
                classes.add(phpClass);
            }

            return classes;
        }
    }
}
