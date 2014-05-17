package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlTokenType;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.comparator.PsiWeightListComparator;
import fr.adrienbrault.idea.symfony2plugin.action.dict.TranslationFileModel;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.form.TranslatorKeyExtractorDialog;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;

import java.awt.*;
import java.util.*;
import java.util.List;

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

        // only since phpstorm 7.1; PlatformDataKeys.PSI_FILE
        Object psiFile = event.getData(DataKey.create("psi.File"));

        if(!(psiFile instanceof TwigFile)) {
            this.setStatus(event, false);
            return;
        }

        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if(editor == null) {
            this.setStatus(event, false);
            return;
        }

        PsiElement psiElement = ((TwigFile) psiFile).findElementAt(editor.getCaretModel().getOffset());
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

        Object psiFile = event.getData(DataKey.create("psi.File"));
        if(!(psiFile instanceof TwigFile)) {
            return;
        }

        final PsiElement psiElement = ((TwigFile) psiFile).findElementAt(editor.getCaretModel().getOffset());
        if(psiElement == null) {
            return;
        }

        IElementType elementType = psiElement.getNode().getElementType();
        if(!(elementType == XmlTokenType.XML_DATA_CHARACTERS || elementType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)) {
            return;
        }

        final String translationText = psiElement.getText();

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(psiElement.getProject());
        final SymfonyBundle symfonyBundle = symfonyBundleUtil.getContainingBundle(psiElement.getContainingFile());

        final List<PsiFile> psiFiles = TranslationUtil.getAllTranslationFiles(psiElement.getProject());

        final List<TranslationFileModel> psiFilesSorted = new ArrayList<TranslationFileModel>();
        for(PsiFile psiFile1: psiFiles) {
            TranslationFileModel psiWeightList = new TranslationFileModel(psiFile1);

            if(symfonyBundle != null && symfonyBundle.isInBundle(psiFile1)) {
                psiWeightList.setSymfonyBundle(symfonyBundle);
                psiWeightList.addWeight(2);
            } else {
                psiWeightList.setSymfonyBundle(symfonyBundleUtil.getContainingBundle(psiFile1));
            }

            String relativePath = psiWeightList.getRelativePath();
            if(relativePath != null && (relativePath.startsWith("src") || relativePath.startsWith("app"))) {
                psiWeightList.addWeight(1);
            }

            psiFilesSorted.add(psiWeightList);
        }

        Collections.sort(psiFilesSorted, new PsiWeightListComparator());

        TranslatorKeyExtractorDialog extractorDialog = new TranslatorKeyExtractorDialog(psiFilesSorted, new TranslatorKeyExtractorDialog.OnOkCallback() {
            @Override
            public void onClick(List<TranslationFileModel> files, final String keyName) {

                PsiDocumentManager.getInstance(psiElement.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument());
                final TextRange range = psiElement.getTextRange();

                String domainName = files.get(0).getPsiFile().getName();
                int indexOfPoint = domainName.indexOf(".");
                if(indexOfPoint > 0) {
                    domainName = domainName.substring(0, indexOfPoint);
                }

                final String finalDomainName = domainName;
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), String.format("{{ '%s'|trans({}, '%s') }}", keyName, finalDomainName));
                        editor.getCaretModel().moveToOffset(range.getStartOffset());
                    }
                });

                for(TranslationFileModel transPsiFile: files) {
                    TranslationInsertUtil.invokeTranslation(keyName, translationText, transPsiFile.getPsiFile(), false);
                }

            }
        });

        extractorDialog.setTitle("Extract Key");
        extractorDialog.setMinimumSize(new Dimension(600, 300));
        extractorDialog.pack();
        extractorDialog.setLocationRelativeTo(null);
        extractorDialog.setVisible(true);


    }

}


