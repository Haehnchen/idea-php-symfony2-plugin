package fr.adrienbrault.idea.symfony2plugin.translation.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.toolwindow.Symfony2SearchForm;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class TranslationInsertUtil {

    public static void invokeTranslation(final String keyName, final String translation, final PsiFile yamlFile, final boolean openFile) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {

            @Override
            public void run() {

                final PsiDocumentManager manager = PsiDocumentManager.getInstance(yamlFile.getProject());
                final Document document = manager.getDocument(yamlFile);
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
                String indent = findIndent(goToPsi.getYamlKeyValue());
                String eol = findEol(goToPsi.getYamlKeyValue());

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
                        insertString += currentKeyName + ": '" + translation + "'";
                    } else {
                        insertString += currentKeyName + ":";
                    }
                }


                // @TODO: check is last array line on contains eol and indent and move above this line

                final String finalInsertString = insertString;
                new WriteCommandAction(yamlFile.getProject()) {
                    @Override
                    protected void run(Result result) throws Throwable {
                        document.insertString(goToPsi.getYamlKeyValue().getTextRange().getEndOffset(), finalInsertString);
                        manager.doPostponedOperationsAndUnblockDocument(document);
                        manager.commitDocument(document);
                    }

                    @Override
                    public String getGroupID() {
                        return "Translation Extraction";
                    }
                }.execute();


                if(!openFile) {
                    return;
                }

                // navigate to new psi element
                // @TODO: jump into quote value
                manager.commitAndRunReadAction(new Runnable() {
                    @Override
                    public void run() {
                        YAMLKeyValue psiElement = YamlKeyFinder.find(yamlDocu, keyName);
                        if(psiElement != null) {
                            Symfony2SearchForm.navigateToPsiElement(psiElement.getValue());
                        } else {
                            Symfony2SearchForm.navigateToPsiElement(yamlFile);
                        }
                    }
                });

            }
        });
    }

    public static String findIndent(PsiElement psiElement) {

        YAMLKeyValue parentYamlKey = PsiTreeUtil.getParentOfType(psiElement, YAMLKeyValue.class);
        if(parentYamlKey != null) {
            return parentYamlKey.getValueIndent();
        }

        PsiElement[] indentPsiElements = PsiTreeUtil.collectElements(psiElement.getContainingFile(), new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement element) {
                return PlatformPatterns.psiElement(YAMLTokenTypes.INDENT).accepts(element);
            }
        });

        if(indentPsiElements.length > 0) {
            return indentPsiElements[0].getText();
        }

        // file without indent; how get default one?
        return "    ";

    }

    public static String findEol(PsiElement psiElement) {

        for(PsiElement child: YamlHelper.getChildrenFix(psiElement)) {
            if(PlatformPatterns.psiElement(YAMLTokenTypes.EOL).accepts(child)) {
                return child.getText();
            }
        }

        PsiElement[] indentPsiElements = PsiTreeUtil.collectElements(psiElement.getContainingFile(), new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement element) {
                return PlatformPatterns.psiElement(YAMLTokenTypes.EOL).accepts(element);
            }
        });

        if(indentPsiElements.length > 0) {
            return indentPsiElements[0].getText();
        }

        return "\n";
    }

}
