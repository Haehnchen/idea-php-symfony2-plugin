package fr.adrienbrault.idea.symfonyplugin.action;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.action.dict.TranslationFileModel;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfonyplugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfonyplugin.translation.form.TranslatorKeyExtractorDialog;
import fr.adrienbrault.idea.symfonyplugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfonyplugin.util.IdeHelper;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtractLanguageAction extends DumbAwareAction {

    public TwigExtractLanguageAction() {
        super("Extract Translation", "Extract Translation Key", Symfony2Icons.SYMFONY);
    }

    public void update(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);

        if (project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            this.setStatus(event, false);
            return;
        }

        PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);
        if(!(psiFile instanceof TwigFile)) {
            this.setStatus(event, false);
            return;
        }

        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if(editor == null) {
            this.setStatus(event, false);
            return;
        }

        // find valid PsiElement context, because only html text is a valid extractor action
        PsiElement psiElement;
        if(editor.getSelectionModel().hasSelection()) {
            psiElement = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());
        } else {
            psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        }

        if(psiElement == null) {
            this.setStatus(event, false);
            return;
        }

        // <a title="TEXT">TEXT</a>
        IElementType elementType = psiElement.getNode().getElementType();
        if(elementType == XmlTokenType.XML_DATA_CHARACTERS || elementType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            this.setStatus(event, true);
        } else {
            this.setStatus(event, false);
        }

    }

    private void setStatus(AnActionEvent event, boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }

    public void actionPerformed(AnActionEvent event) {

        final Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if(editor == null) {
            return;
        }

        PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);
        if(!(psiFile instanceof TwigFile)) {
            return;
        }

        final Project project = ((TwigFile) psiFile).getProject();
        String translationText = editor.getSelectionModel().getSelectedText();

        int startOffset;
        int endOffset;
        int caretOffset = editor.getCaretModel().getOffset();

        if(translationText != null) {
            startOffset = editor.getSelectionModel().getSelectionStart();
            endOffset = editor.getSelectionModel().getSelectionEnd();
        } else {

            // use dont selected text, so find common PsiElement
            PsiElement psiElement = psiFile.findElementAt(caretOffset);
            if(psiElement == null) {
                return;
            }

            IElementType elementType = psiElement.getNode().getElementType();
            if(!(elementType == XmlTokenType.XML_DATA_CHARACTERS || elementType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)) {
                return;
            }

            startOffset = psiElement.getTextRange().getStartOffset();
            endOffset = psiElement.getTextRange().getEndOffset();
            translationText = psiElement.getText();
        }

        final Set<String> domainNames = TranslationUtil.getTranslationDomainLookupElements(project).stream()
            .map(LookupElement::getLookupString)
            .collect(Collectors.toCollection(TreeSet::new));

        // get default domain on twig tag
        // also pipe it to insert handler; to append it as parameter

        // scope to search translation domain
        PsiElement transDefaultScope = psiFile.findElementAt(caretOffset);
        if(transDefaultScope == null) {
            transDefaultScope = psiFile;
        }

        PsiElement element = TwigUtil.getElementOnTwigViewProvider(transDefaultScope);
        TwigUtil.DomainScope twigFileDomainScope = TwigUtil.getTwigFileDomainScope(element != null ? element : transDefaultScope);

        // we want to have mostly used domain preselected
        final String defaultDomain = twigFileDomainScope.getDefaultDomain();
        final String reselectedDomain = twigFileDomainScope.getDomain();

        String defaultKey = null;
        if(translationText.length() < 15) {
            defaultKey = translationText.toLowerCase().replace(" ", ".");
        }

        final int finalStartOffset = startOffset;
        final int finalEndOffset = endOffset;
        final String finalTranslationText = translationText;
        TranslatorKeyExtractorDialog extractorDialog = new TranslatorKeyExtractorDialog(project, psiFile, domainNames, defaultKey, reselectedDomain, new MyOnOkCallback(project, editor, defaultDomain, finalStartOffset, finalEndOffset, finalTranslationText));

        extractorDialog.setTitle("Symfony: Extract Translation Key");
        extractorDialog.setMinimumSize(new Dimension(600, 200));
        extractorDialog.pack();
        extractorDialog.setLocationRelativeTo(editor.getComponent());
        extractorDialog.setVisible(true);
        extractorDialog.setIconImage(Symfony2Icons.getImage(Symfony2Icons.SYMFONY));

    }

    private static class MyOnOkCallback implements TranslatorKeyExtractorDialog.OnOkCallback {
        private final Project project;
        private final Editor editor;
        private final String finalDefaultDomain;
        private final int finalStartOffset;
        private final int finalEndOffset;
        private final String finalTranslationText;

        MyOnOkCallback(Project project, Editor editor, String finalDefaultDomain, int finalStartOffset, int finalEndOffset, String finalTranslationText) {
            this.project = project;
            this.editor = editor;
            this.finalDefaultDomain = finalDefaultDomain;
            this.finalStartOffset = finalStartOffset;
            this.finalEndOffset = finalEndOffset;
            this.finalTranslationText = finalTranslationText;
        }

        @Override
        public void onClick(List<TranslationFileModel> files, final String keyName, final String domain, boolean navigateTo) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

            // insert Twig trans key
            CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
                String insertString;

                // check for file context domain
                if(finalDefaultDomain.equals(domain)) {
                    insertString = String.format("{{ '%s'|trans }}", keyName);
                } else {
                    insertString = String.format("{{ '%s'|trans({}, '%s') }}", keyName, domain);
                }

                editor.getDocument().replaceString(finalStartOffset, finalEndOffset, insertString);
                editor.getCaretModel().moveToOffset(finalEndOffset);
            }), "Twig Translation Insert " + keyName, null);

            Collection<PsiElement> targets = new ArrayList<>();

            // so finally insert it; first file can be a navigation target
            for(TranslationFileModel transPsiFile: files) {
                PsiFile psiFile = transPsiFile.getPsiFile();

                CommandProcessor.getInstance().executeCommand(psiFile.getProject(), () -> ApplicationManager.getApplication().runWriteAction(() ->
                    ContainerUtil.addIfNotNull(targets, TranslationInsertUtil.invokeTranslation(psiFile, keyName, finalTranslationText))),
                    "Translation Insert " + psiFile.getName(), null
                );
            }

            if(navigateTo && targets.size() > 0) {
                PsiDocumentManager.getInstance(project).commitAndRunReadAction(() ->
                    IdeHelper.navigateToPsiElement(targets.iterator().next())
                );
            }
        }
    }
}


