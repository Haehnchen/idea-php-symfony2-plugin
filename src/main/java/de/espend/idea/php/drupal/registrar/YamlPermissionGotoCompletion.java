package de.espend.idea.php.drupal.registrar;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.index.PermissionIndex;
import de.espend.idea.php.drupal.registrar.utils.YamlRegistrarUtil;
import de.espend.idea.php.drupal.utils.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlPermissionGotoCompletion implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(YamlElementPatternHelper.getSingleLineScalarKey("_permission"), psiElement -> {
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
            return IndexUtil.getIndexedKeyLookup(getProject(), PermissionIndex.KEY);
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            String text = YamlRegistrarUtil.getYamlScalarKey(psiElement);
            if(text == null) {
                return Collections.emptyList();
            }

            return getPermissionPsiElements(getProject(), text);
        }
    }

    @NotNull
    public static Collection<PsiElement> getPermissionPsiElements(@NotNull Project project, @NotNull String text) {
        Collection<PsiElement> targets = new ArrayList<>();

        Collection<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(PermissionIndex.KEY, new HashSet<>(Collections.singletonList(text)), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, GlobalSearchScope.allScope(project));

        for (VirtualFile virtualFile : virtualFiles) {
            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if(!(file instanceof YAMLFile)) {
                continue;
            }

            ContainerUtil.addIfNotNull(targets, YAMLUtil.getQualifiedKeyInFile((YAMLFile) file, text));
        }

        return targets;
    }
}
