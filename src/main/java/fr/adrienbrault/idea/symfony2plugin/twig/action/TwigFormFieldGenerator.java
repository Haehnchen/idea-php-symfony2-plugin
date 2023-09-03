package fr.adrienbrault.idea.symfony2plugin.twig.action;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFormFieldGenerator extends CodeInsightAction {
    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        boolean b = Symfony2ProjectComponent.isEnabled(project) && (
            file instanceof TwigFile
                || (file instanceof HtmlFileImpl && file.getName().toLowerCase().endsWith(".twig"))
                || TwigUtil.getInjectedTwigElement(file, editor) != null
        );

        if (!b) {
            return false;
        }

        PsiElement psiElement = TwigUtil.getInjectedTwigElement(file, editor);
        if (psiElement == null) {
            return false;
        }

        return TwigTypeResolveUtil.collectScopeVariables(psiElement).entrySet()
            .stream()
            .anyMatch(entry -> FormFieldResolver.isFormView(PhpIndex.getInstance(project).completeType(project, PhpType.from(entry.getValue().getTypes().toArray(new String[0])), new HashSet<>())));
    }

    protected CodeInsightActionHandler getHandler() {
        return new TwigFormFieldGenerator.MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
            PsiElement psiElement = TwigUtil.getInjectedTwigElement(psiFile, editor);
            if (psiElement == null) {
                return;
            }

            Collection<JBFormFieldItem> phpClasses = new ArrayList<>();

            for (Map.Entry<String, PsiVariable> entry : TwigTypeResolveUtil.collectScopeVariables(psiElement).entrySet()) {
                PhpType phpType = PhpIndex.getInstance(project).completeType(project, PhpType.from(entry.getValue().getTypes().toArray(new String[0])), new HashSet<>());
                if (!FormFieldResolver.isFormView(phpType)) {
                    continue;
                }

                PsiElement element = entry.getValue().getElement();
                if (element == null)  {
                    continue;
                }

                for (PhpClass phpClass : FormFieldResolver.getFormTypeFromFormFactory(element)) {
                    phpClasses.add(new JBFormFieldItem(entry.getKey(), phpClass));
                }
            }

            if (phpClasses.size() == 1) {
                extracted(project, editor, phpClasses.stream().iterator().next());
                return;
            }

            JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<>(phpClasses))
                .setTitle("Symfony: Select FormType")
                .setItemChosenCallback(strings -> WriteCommandAction.runWriteCommandAction(project, "", null, () -> {
                    extracted(project, editor, strings);
                }))
                .createPopup()
                .showInBestPositionFor(editor);
        }

        private static void extracted(@NotNull Project project, @NotNull Editor editor, @NotNull TwigFormFieldGenerator.JBFormFieldItem next) {
            Collection<String> fields = new HashSet<>();
            FormFieldResolver.visitFormReferencesFields(next.phpClass(), twigTypeContainer -> fields.add(twigTypeContainer.getStringElement()));

            JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<>(fields))
                .setTitle(String.format("Symfony: Select Form Fields \"%s\"", StringUtils.abbreviate(next.phpClass.getName(), 20)))
                .setItemsChosenCallback(strings -> WriteCommandAction.runWriteCommandAction(project, "", null, () -> {
                    StringBuilder s = new StringBuilder();

                    for (String string : strings) {
                        s.append(String.format("{{ form_row(%s.%s) }}\n", next.key, string));
                    }

                    PhpInsertHandlerUtil.insertStringAtCaret(editor, s.toString());
                }))
                .createPopup()
                .showInBestPositionFor(editor);
        }
    }

    public record JBFormFieldItem(@NotNull String key, @NotNull PhpClass phpClass) {
        @Override
        public String toString() {
            return key + " => " + phpClass.getName();
        }
    }
}
