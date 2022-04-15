package fr.adrienbrault.idea.symfony2plugin.twig.action;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationGeneratorAction extends CodeInsightAction {
    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return Symfony2ProjectComponent.isEnabled(project) && (
            file instanceof TwigFile
            || (file instanceof HtmlFileImpl && file.getName().toLowerCase().endsWith(".twig"))
            || TwigUtil.getInjectedTwigElement(file, editor) != null
        );
    }

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
            PsiElement psiElement = TwigUtil.getInjectedTwigElement(psiFile, editor);
            if(psiElement == null) {
                return;
            }

            PsiFile containingFile = psiElement.getContainingFile();
            if(!(containingFile instanceof TwigFile)) {
                return;
            }

            PsiElement element = TwigUtil.getElementOnTwigViewProvider(psiElement);
            TwigUtil.DomainScope twigFileDomainScope = TwigUtil.getTwigFileDomainScope(element != null ? element : psiElement);

            final String defaultDomain = twigFileDomainScope.getDefaultDomain();
            final String domain = twigFileDomainScope.getDomain();

            final List<String> list = TranslationUtil.getTranslationLookupElementsOnDomain(project, domain)
                .stream()
                .map(LookupElement::getLookupString)
                .sorted()
                .collect(Collectors.toList());

            JBPopupFactory.getInstance().createPopupChooserBuilder(list)
                .setTitle(String.format("Symfony: Translations \"%s\"", StringUtils.abbreviate(domain, 20)))
                .setItemChosenCallback(selectedValue -> WriteCommandAction.runWriteCommandAction(editor.getProject(), String.format("Symfony: Add Translation \"%s\"", StringUtils.abbreviate(selectedValue, 20)), null, () -> {
                    String s;

                    if (!domain.equals(defaultDomain)) {
                        s = String.format("{{ '%s'|trans({}, '%s') }}", selectedValue, domain);
                    } else {
                        s = String.format("{{ '%s'|trans }}", selectedValue);
                    }

                    PhpInsertHandlerUtil.insertStringAtCaret(editor, s);
                }))
                .createPopup()
                .showInBestPositionFor(editor);
        }
    }
}
