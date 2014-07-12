package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTokenType;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.action.comparator.ValueComparator;
import fr.adrienbrault.idea.symfony2plugin.action.dict.TranslationFileModel;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.form.TranslatorKeyExtractorDialog;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;

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
        if(StringUtils.isBlank(translationText)) {
            return;
        }

        final Set<String> domainNames = new TreeSet<String>();
        for(LookupElement lookupElement: TranslationUtil.getTranslationDomainLookupElements(psiElement.getProject())) {
            domainNames.add(lookupElement.getLookupString());
        }

        // get default domain on twig tag
        // also pipe it to insert handler; to append it as parameter
        String defaultDomain = TwigUtil.getTwigFileTransDefaultDomain(psiElement.getContainingFile());
        if(defaultDomain == null) {
            defaultDomain = "messages";
        }

        TreeMap<String, Integer> sortedMap = getPossibleDomainTreeMap((TwigFile) psiFile, domainNames);

        // we want to have mostly used domain preselected
        String reselectedDomain = defaultDomain;
        if(sortedMap.size() > 0) {
            reselectedDomain = sortedMap.firstKey();
        }

        String defaultKey = null;
        if(translationText.length() < 15) {
            defaultKey = translationText.toLowerCase().replace(" ", ".");
        }

        final String finalDefaultDomain = defaultDomain;
        TranslatorKeyExtractorDialog extractorDialog = new TranslatorKeyExtractorDialog(psiElement.getProject(), (PsiFile) psiFile, domainNames, defaultKey, reselectedDomain, new TranslatorKeyExtractorDialog.OnOkCallback() {
            @Override
            public void onClick(List<TranslationFileModel> files, final String keyName, final String domain, boolean navigateTo) {

                PsiDocumentManager.getInstance(psiElement.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument());
                final TextRange range = psiElement.getTextRange();

                new WriteCommandAction(psiElement.getProject()) {
                    @Override
                    protected void run(Result result) throws Throwable {
                        String insertString;

                        // check for file context domain
                        if(finalDefaultDomain.equals(domain)) {
                            insertString = String.format("{{ '%s'|trans }}", keyName);
                        } else {
                            insertString = String.format("{{ '%s'|trans({}, '%s') }}", keyName, domain);
                        }

                        editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), insertString);
                        editor.getCaretModel().moveToOffset(range.getStartOffset());
                    }

                    @Override
                    public String getGroupID() {
                        return "Translation Extraction";
                    }
                }.execute();

                // so finally insert it; first file can be a navigation target
                for(TranslationFileModel transPsiFile: files) {
                    TranslationInsertUtil.invokeTranslation(keyName, translationText, transPsiFile.getPsiFile(), navigateTo);
                    navigateTo = false;
                }

            }
        });

        extractorDialog.setTitle("Symfony2: Extract Translation Key");
        extractorDialog.setMinimumSize(new Dimension(600, 200));
        extractorDialog.pack();
        extractorDialog.setLocationRelativeTo(null);
        extractorDialog.setVisible(true);

    }

    private TreeMap<String, Integer> getPossibleDomainTreeMap(TwigFile psiFile, final Set<String> domainNames) {

        final Map<String, Integer> found = new HashMap<String, Integer>();

        // visit every trans or transchoice to get possible domain names
        PsiTreeUtil.collectElements(psiFile, new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement psiElement) {

                if (TwigHelper.getTransDomainPattern().accepts(psiElement)) {
                    PsiElement psiElementTrans = PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")));
                    if (psiElementTrans != null && TwigHelper.getTwigMethodString(psiElementTrans) != null) {
                        String text = psiElement.getText();
                        if (StringUtils.isNotBlank(text) && domainNames.contains(text)) {
                            if (found.containsKey(text)) {
                                found.put(text, found.get(text) + 1);
                            } else {
                                found.put(text, 1);
                            }
                        }
                    }
                }

                return false;
            }
        });

        // sort in found integer value
        ValueComparator vc =  new ValueComparator(found);
        TreeMap<String, Integer> sortedMap = new TreeMap<String, Integer>(vc);
        sortedMap.putAll(found);

        return sortedMap;
    }

}


