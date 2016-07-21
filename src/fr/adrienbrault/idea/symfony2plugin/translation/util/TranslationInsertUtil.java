package fr.adrienbrault.idea.symfony2plugin.translation.util;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class TranslationInsertUtil {

    public static void invokeTranslation(@NotNull final Editor editor, @NotNull final String keyName, @NotNull final String translation, @NotNull final YAMLFile yamlFile, final boolean openFile) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            final String[] keys = keyName.split("\\.");
            if(YamlHelper.insertKeyIntoFile(yamlFile, "'" + translation + "'", keys) == null) {
                HintManager.getInstance().showErrorHint(editor, "Error adding key");
                return;
            }

            if(!openFile) {
                return;
            }

            PsiDocumentManager manager = PsiDocumentManager.getInstance(yamlFile.getProject());

            // navigate to new psi element
            // @TODO: jump into quote value
            manager.commitAndRunReadAction(new MyKeyNavigationRunnable(yamlFile, keys));
        });
    }

    /**
     * Remove TODO; moved to core
     */
    @Deprecated
    @NotNull
    public static String findEol(@NotNull PsiElement psiElement) {

        for(PsiElement child: YamlHelper.getChildrenFix(psiElement)) {
            if(PlatformPatterns.psiElement(YAMLTokenTypes.EOL).accepts(child)) {
                return child.getText();
            }
        }

        PsiElement[] indentPsiElements = PsiTreeUtil.collectElements(psiElement.getContainingFile(), element ->
            PlatformPatterns.psiElement(YAMLTokenTypes.EOL).accepts(element)
        );

        if(indentPsiElements.length > 0) {
            return indentPsiElements[0].getText();
        }

        return "\n";
    }

    private static class MyKeyNavigationRunnable implements Runnable {
        private final PsiFile yamlFile;
        private final String[] keys;

        MyKeyNavigationRunnable(YAMLFile yamlFile, String[] keys) {
            this.yamlFile = yamlFile;
            this.keys = keys;
        }

        @Override
        public void run() {
            YAMLKeyValue psiElement = YAMLUtil.getQualifiedKeyInFile((YAMLFile) yamlFile, keys);
            if(psiElement != null && psiElement.getValue() != null) {
                IdeHelper.navigateToPsiElement(psiElement.getValue());
            } else {
                IdeHelper.navigateToPsiElement(yamlFile);
            }
        }
    }
}
