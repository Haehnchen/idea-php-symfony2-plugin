package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.codeInsight.navigation.PsiTargetNavigator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationKeyIntentionAndQuickFixAction extends IntentionAndQuickFixAction {
    @NotNull
    private final String key;

    @NotNull
    private final String domain;

    @NotNull
    private final DomainCollector domainCollector;

    public TranslationKeyIntentionAndQuickFixAction(@NotNull String key, @NotNull String domain) {
        this(key, domain, new AllDomainCollector());
    }

    public TranslationKeyIntentionAndQuickFixAction(@NotNull String key, @NotNull String domain, @NotNull DomainCollector domainCollector) {
        this.key = key;
        this.domain = domain;
        this.domainCollector = domainCollector;
    }

    @NotNull
    @Override
    public String getName() {
        return "Symfony: Add translations";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    private String getPresentableName(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        // try to find suitable presentable filename
        String filename = virtualFile.getPath();

        String relativePath = VfsUtil.getRelativePath(virtualFile, ProjectUtil.getProjectDir(project), '/');
        if(relativePath != null) {
            filename =  relativePath;
        }

        return StringUtils.abbreviate(filename, 180);
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor) {
        if(editor == null || IntentionPreviewUtils.isIntentionPreviewActive()) {
            return;
        }

        List<PsiFile> files = new ArrayList<>();

        for(PsiFile translationPsiFile: this.domainCollector.collect(project, key, domain)) {
            if(translationPsiFile instanceof YAMLFile || TranslationUtil.isSupportedXlfFile(translationPsiFile)) {
                String relativePath = VfsUtil.getRelativePath(translationPsiFile.getVirtualFile(), ProjectUtil.getProjectDir(project), '/');

                // sort collection. eg vendor last
                if(relativePath != null && (relativePath.startsWith("app") || relativePath.startsWith("src"))) {
                    files.add(0, translationPsiFile);
                } else {
                    files.add(translationPsiFile);
                }
            }
        }

        if(files.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "Ops, no domain file found");
            return;
        }

        new PsiTargetNavigator<>(files)
            .presentationProvider(file -> TargetPresentation.builder(getPresentableName(project, file.getVirtualFile())).presentation())
            .navigate(editor, "Symfony: Translation files", selectedFile -> {
                CommandProcessor.getInstance().executeCommand(selectedFile.getProject(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    TranslationInsertUtil.invokeTranslation(selectedFile, key, domain);
                }), "Translation insert " + selectedFile.getName(), null);

                return true;
            });
    }

    public interface DomainCollector {
        @NotNull
        Collection<PsiFile> collect(@NotNull Project project, @NotNull String key, @NotNull String domain);
    }

    private static class AllDomainCollector implements DomainCollector {
        @Override
        @NotNull
        public Collection<PsiFile> collect(@NotNull Project project, @NotNull String key, @NotNull String domain) {
            return TranslationUtil.getDomainPsiFiles(project, domain);
        }
    }
}
