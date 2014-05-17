package fr.adrienbrault.idea.symfony2plugin.action.dict;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.Nullable;

public class TranslationFileModel {

    final private PsiFile psiFile;
    private int weight = 0;
    private boolean enabled = false;
    private SymfonyBundle symfonyBundle;

    public TranslationFileModel(PsiFile psiFile) {
        this.psiFile = psiFile;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    public int getWeight() {
        return weight;
    }

    public void addWeight(int weight) {
        this.weight += weight;
    }

    @Nullable
    public String getRelativePath() {
        return VfsUtil.getRelativePath(psiFile.getVirtualFile(), psiFile.getProject().getBaseDir(), '/');
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public SymfonyBundle getSymfonyBundle() {
        return symfonyBundle;
    }

    public void setSymfonyBundle(@Nullable SymfonyBundle symfonyBundle) {
        this.symfonyBundle = symfonyBundle;
    }

}
