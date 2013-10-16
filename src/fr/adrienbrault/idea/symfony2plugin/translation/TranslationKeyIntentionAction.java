package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import fr.adrienbrault.idea.symfony2plugin.toolwindow.Symfony2SearchForm;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class TranslationKeyIntentionAction extends BaseIntentionAction {

    protected PsiFile psiFile;
    protected String keyName;

    public TranslationKeyIntentionAction(PsiFile psiFile, String keyName) {
        this.psiFile = psiFile;
        this.keyName = keyName;
    }

    @NotNull
    @Override
    public String getText() {
        return "Create Translation Key: " + psiFile.getName();
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
                VirtualFile virtualFile = TranslationKeyIntentionAction.this.psiFile.getVirtualFile();
                if(virtualFile == null) {
                    return;
                }

                PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
                Document document = manager.getDocument(psiFile);
                if (document != null) {
                    manager.commitDocument(document);
                }

                final PsiElement yamlDocu = PsiTreeUtil.findChildOfType(psiFile, YAMLDocument.class);
                if(yamlDocu == null) {
                    return;
                }

                final YamlKeyFinder.MatchedKey goToPsi = YamlKeyFinder.findLastValueElement(yamlDocu, keyName);
                if(goToPsi == null) {
                    return;
                }

                //CodeStyleManager.getInstance(project).reformatNewlyAddedElement(parent.getNode(), newDoc.getNode());

                // find indent inside current array value
                PsiElement psiIndent = PsiElementUtils.getChildrenOfType(goToPsi.getYamlKeyValue(), PlatformPatterns.psiElement(YAMLTokenTypes.INDENT));
                String indent = "    ";
                if(psiIndent != null) {
                    indent = "";
                }

                // find eol key!?
                String eol = "\n";
                PsiElement psiEol = PsiElementUtils.getChildrenOfType(goToPsi.getYamlKeyValue(), PlatformPatterns.psiElement(YAMLTokenTypes.EOL));
                if(psiEol != null) {
                    eol = psiEol.getText();
                }

                String string = "";
                int current = 0;

                // we support multiple depth keys here: "key.key.value" and "value"
                for(String keyName: goToPsi.getMissingKeys()) {

                    // add indent of children
                    // @TODO: find indent size
                    string += eol + StringUtils.repeat("    ", current + goToPsi.getStartDepth());

                    if(goToPsi.getMissingKeys().length - 1 == current) {
                        string += keyName + ": ''";
                    } else {
                        string += keyName + ":";
                    }
                    current++;
                }

                // @TODO: check is last array line on contains eol and indent and move above this line
                document.insertString(goToPsi.getYamlKeyValue().getTextRange().getEndOffset(), string);
                manager.doPostponedOperationsAndUnblockDocument(document);
                manager.commitDocument(document);

                // navigate to new psi element
                // @TODO: jump into quote value
                manager.commitAndRunReadAction(new Runnable() {
                    @Override
                    public void run() {
                        YAMLKeyValue psiElement = YamlKeyFinder.find(yamlDocu, keyName);
                        if(psiElement != null) {
                            Symfony2SearchForm.navigateToPsiElement(psiElement.getValue());
                        }
                    }
                });

            }
        });
    }
}
