package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

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

        return "Create Translation Key: " + filename;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony2";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {

            @Override
            public void run() {
                VirtualFile virtualFile = TranslationKeyIntentionAction.this.yamlFile.getVirtualFile();
                if(virtualFile == null) {
                    return;
                }

                PsiDocumentManager manager = PsiDocumentManager.getInstance(yamlFile.getProject());
                Document document = manager.getDocument(yamlFile);
                if (document != null) {
                    manager.commitDocument(document);
                }

                final PsiElement yamlDocu = PsiTreeUtil.findChildOfType(yamlFile, YAMLDocument.class);
                if(yamlDocu == null) {
                    return;
                }

                final YamlKeyFinder.MatchedKey goToPsi = YamlKeyFinder.findLastValueElement(yamlDocu, keyName);
                if(goToPsi == null) {
                    return;
                }

                // search indent and EOL value
                String indent = TranslationInsertUtil.findIndent(goToPsi.getYamlKeyValue());
                String eol = TranslationInsertUtil.findEol(goToPsi.getYamlKeyValue());

                String currentIndentOffset = "";
                PsiElement lastKnownPsiElement = goToPsi.getYamlKeyValue();
                if(lastKnownPsiElement instanceof YAMLKeyValue) {
                    currentIndentOffset = ((YAMLKeyValue) lastKnownPsiElement).getValueIndent();
                }

                String insertString = "";
                String[] missingKeys = goToPsi.getMissingKeys();
                for ( int i = 0; i < missingKeys.length; i++ ) {
                    String currentKeyName = missingKeys[i];

                    // add indent
                    insertString += eol + currentIndentOffset + StringUtils.repeat(indent, i);

                    // on last key name we dont need new lines
                    if(goToPsi.getMissingKeys().length - 1 == i) {
                        // we are developer should other translate it
                        insertString += currentKeyName + ": '" + keyName + "'";
                    } else {
                        insertString += currentKeyName + ":";
                    }
                }


                // @TODO: check is last array line on contains eol and indent and move above this line
                document.insertString(goToPsi.getYamlKeyValue().getTextRange().getEndOffset(), insertString);
                manager.doPostponedOperationsAndUnblockDocument(document);
                manager.commitDocument(document);

                // navigate to new psi element
                // @TODO: jump into quote value
                manager.commitAndRunReadAction(new Runnable() {
                    @Override
                    public void run() {
                        YAMLKeyValue psiElement = YamlKeyFinder.find(yamlDocu, keyName);
                        if(psiElement != null) {
                            IdeHelper.navigateToPsiElement(psiElement.getValue());
                        } else {
                            IdeHelper.navigateToPsiElement(yamlFile);
                        }
                    }
                });

            }
        });
    }

}
