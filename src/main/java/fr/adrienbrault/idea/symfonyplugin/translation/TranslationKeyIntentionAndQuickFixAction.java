package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBList;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;

import javax.swing.*;
import java.awt.*;
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

        String relativePath = VfsUtil.getRelativePath(virtualFile, project.getBaseDir(), '/');
        if(relativePath != null) {
            filename =  relativePath;
        }

        return StringUtils.abbreviate(filename, 180);
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor) {
        if(editor == null) {
            return;
        }

        List<PsiFile> files = new ArrayList<>();

        for(PsiFile translationPsiFile: this.domainCollector.collect(project, key, domain)) {
            if(translationPsiFile instanceof YAMLFile || TranslationUtil.isSupportedXlfFile(translationPsiFile)) {
                String relativePath = VfsUtil.getRelativePath(translationPsiFile.getVirtualFile(), project.getBaseDir(), '/');

                // sort collection. eg vendor last
                if(relativePath != null && (relativePath.startsWith("app") || relativePath.startsWith("src"))) {
                    files.add(0, translationPsiFile);
                } else {
                    files.add(translationPsiFile);
                }
            }
        }

        if(files.size() == 0) {
            HintManager.getInstance().showErrorHint(editor, "Ops, no domain file found");
            return;
        }

        JBList<PsiFile> list = new JBList<>(files);

        list.setCellRenderer(new JBList.StripedListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (renderer instanceof JLabel && value instanceof PsiFile) {
                    ((JLabel) renderer).setText(getPresentableName(project, ((PsiFile) value).getVirtualFile()));
                }

                return renderer;
            }
        });

        JBPopupFactory.getInstance().createListPopupBuilder(list)
            .setTitle("Symfony: Translation files")
            .setItemChoosenCallback(() -> {
                PsiFile selectedFile = list.getSelectedValue();

                CommandProcessor.getInstance().executeCommand(selectedFile.getProject(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    TranslationInsertUtil.invokeTranslation(selectedFile, key, domain);
                }), "Translation insert " + selectedFile.getName(), null);
            })
            .createPopup()
            .showInBestPositionFor(editor);
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