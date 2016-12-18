package fr.adrienbrault.idea.symfony2plugin.action.dict;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationFileModel {

    final private PsiFile psiFile;
    private int weight = 0;
    private boolean enabled = false;
    private SymfonyBundle symfonyBundle;
    private boolean boldness = false;

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

    public String getDomain() {
        String domainName = this.psiFile.getName();
        int indexOfPoint = domainName.indexOf(".");
        if(indexOfPoint > 0) {
            domainName = domainName.substring(0, indexOfPoint);
        }

        return domainName;
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

    public TranslationFileModel setBoldness(boolean bold) {
        this.boldness = bold;
        return this;
    }

    public boolean isBoldness() {
        return boldness;
    }


}
