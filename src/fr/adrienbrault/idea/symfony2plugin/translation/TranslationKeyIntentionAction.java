package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

public class TranslationKeyIntentionAction extends BaseIntentionAction {

    protected YAMLFile yamlFile;
    protected String keyName;

    /**
     *
     * @param yamlFile Translation file as yaml
     * @param keyName key name like "translation" or "translation.sub.name"
     */
    public TranslationKeyIntentionAction(YAMLFile yamlFile, String keyName) {
        this.yamlFile = yamlFile;
        this.keyName = keyName;
    }

    @NotNull
    @Override
    public String getText() {
        String filename = yamlFile.getName();

        // try to find suitable presentable filename
        VirtualFile virtualFile = yamlFile.getVirtualFile();
        if(virtualFile != null) {
            filename = virtualFile.getPath();
            String relativePath = VfsUtil.getRelativePath(virtualFile, yamlFile.getProject().getBaseDir(), '/');
            if(relativePath != null) {
                filename =  relativePath;
            }
        }

        return "Create translation key: " + filename;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {

        VirtualFile virtualFile = TranslationKeyIntentionAction.this.yamlFile.getVirtualFile();
        if(virtualFile == null) {
            return;
        }

        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if(!(psiFile instanceof YAMLFile)) {
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            TranslationInsertUtil.invokeTranslation(editor, keyName, keyName, (YAMLFile) psiFile, true);
        });
    }

}
