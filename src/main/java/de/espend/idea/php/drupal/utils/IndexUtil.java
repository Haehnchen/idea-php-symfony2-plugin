package de.espend.idea.php.drupal.utils;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.drupal.DrupalIcons;
import de.espend.idea.php.drupal.index.ConfigEntityTypeAnnotationIndex;
import de.espend.idea.php.drupal.index.MenuIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IndexUtil {

    @NotNull
    public static Collection<PhpClass> getFormClassForId(@NotNull Project project, @NotNull String id)  {
        Collection<PhpClass> phpClasses = new ArrayList<>();

        for (String key : SymfonyProcessors.createResult(project, ConfigEntityTypeAnnotationIndex.KEY)) {
            if(!id.equals(key)) {
                continue;
            }

            for (String value : FileBasedIndex.getInstance().getValues(ConfigEntityTypeAnnotationIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                phpClasses.addAll(PhpElementsUtil.getClassesInterface(project, value));
            }
        }

        return phpClasses;
    }

    @NotNull
    public static Collection<PsiElement> getMenuForId(@NotNull Project project, @NotNull String text)  {
        Collection<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(MenuIndex.KEY, new HashSet<>(Collections.singletonList(text)), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, GlobalSearchScope.allScope(project));

        Collection<PsiElement> targets = new ArrayList<>();

        for (VirtualFile virtualFile : virtualFiles) {
            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if(!(file instanceof YAMLFile)) {
                continue;
            }

            ContainerUtil.addIfNotNull(targets, YAMLUtil.getQualifiedKeyInFile((YAMLFile) file, text));
        }

        return targets;
    }

    @NotNull
    public static Collection<LookupElement> getIndexedKeyLookup(@NotNull Project project, @NotNull ID<String, ?> var1) {
        Collection<LookupElement> lookupElements = new ArrayList<>();

        lookupElements.addAll(SymfonyProcessors.createResult(project, var1).stream().map(
            s -> LookupElementBuilder.create(s).withIcon(DrupalIcons.DRUPAL)).collect(Collectors.toList())
        );

        return lookupElements;
    }

    public static boolean isValidForIndex(@NotNull FileContent inputData, @NotNull PsiFile psiFile) {

        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        VirtualFile baseDir = inputData.getProject().getBaseDir();
        if(baseDir == null) {
            return false;
        }

        // is Test file in path name
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), baseDir, '/');
        if(relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Fixtures/"))) {
            return false;
        }

        return true;
    }
}
