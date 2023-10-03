package fr.adrienbrault.idea.symfony2plugin.form.action.generator;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.FormUnderscoreMethodReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormBuilderFieldGeneratorAction extends CodeInsightAction {
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (!(file instanceof PhpFile) || !Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        Method method = PsiTreeUtil.findElementOfClassAtOffset(file, editor.getCaretModel().getOffset(), Method.class, false);
        return method != null
            && "buildForm".equals(method.getName())
            && PhpElementsUtil.isMethodInstanceOf(method, "\\Symfony\\Component\\Form\\FormTypeInterface", "buildForm");
    }

    @Override
    protected @NotNull CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
            Method method = PsiTreeUtil.findElementOfClassAtOffset(psiFile, editor.getCaretModel().getOffset(), Method.class, false);
            if (method == null) {
                return;
            }

            PhpClass phpClass = FormOptionsUtil.getFormPhpClassFromContext(method);
            if (phpClass == null) {
                IdeHelper.showErrorHintIfAvailable(editor, "No data_class option context found");
                return;
            }

            List<JBFormFieldItem> jbFormFieldItems = new ArrayList<>();
            FormUnderscoreMethodReference.visitPropertyPath(phpClass, pair -> jbFormFieldItems.add(new JBFormFieldItem(pair.getFirst(), pair.getSecond())));

            JBPopupFactory.getInstance().createPopupChooserBuilder(jbFormFieldItems)
                .setTitle("Symfony: Select Fields")
                .setItemsChosenCallback(strings -> WriteCommandAction.runWriteCommandAction(project, "", null, () -> {
                    insertSelectedNamedElements(project, method, editor, strings);
                }))
                .createPopup()
                .showInBestPositionFor(editor);

        }

        private record JBFormFieldItem(@NotNull String key, @NotNull PhpNamedElement phpNamedElement) {
            @Override
            public String toString() {
                return key;
            }
        }

        private void insertSelectedNamedElements(@NotNull Project project, Method method, @NotNull Editor editor, @NotNull Collection<? extends JBFormFieldItem> strings) {
            String formBuilderVariable = findFormBuilderVariable(project, method);
            if (formBuilderVariable == null) {
                return;
            }

            String content = "";

            PhpIndex instance = PhpIndex.getInstance(project);

            for (JBFormFieldItem string : strings) {
                Pair<String, Map<String, String>> guessedFormFieldParameters = FormUtil.getGuessedFormFieldParameters(instance, project, string.key, string.phpNamedElement);
                String typeClass = guessedFormFieldParameters.getFirst();
                Map<String, String> options = guessedFormFieldParameters.getSecond();

                content += "$%s->add('%s'".formatted(formBuilderVariable, string.key);

                if (typeClass != null) {
                    typeClass = PhpElementsUtil.insertUseIfNecessary(method, typeClass);
                    content += ", " + typeClass + "::class";
                }

                Set<Map.Entry<String, String>> entries = options.entrySet();
                if (entries.size() > 0) {
                    content += ", [";

                    List<String> opts = new ArrayList<>();
                    for (Map.Entry<String, String> entry : entries) {
                        if (entry.getKey().equals("class")) {
                            String classUse = PhpElementsUtil.insertUseIfNecessary(method, entry.getValue());
                            opts.add("'%s' => %s::class".formatted(entry.getKey(), classUse));
                        } else {
                            opts.add("'%s' => %s".formatted(entry.getKey(), entry.getValue()));
                        }
                    }

                    content += StringUtils.join(opts, ", ");

                    content += "]";
                }

                content += ");\n";
            }

            int caretModel = editor.getCaretModel().getOffset();

            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

            PhpInsertHandlerUtil.insertStringAtCaret(editor, content);

            CodeStyleManager.getInstance(project).reformatText(method.getContainingFile(), caretModel, caretModel + content.length());
            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        }

        @Nullable
        private static String findFormBuilderVariable(@NotNull Project project, @NotNull Method method) {
            return Arrays.stream(method.getParameters())
                .filter(parameter -> parameter.getType().isConvertibleFrom(project, new PhpType().add("\\Symfony\\Component\\Form\\FormBuilderInterface")))
                .findFirst().map(Parameter::getName)
                .orElse(null);
        }
    }

    private record JBFormFieldItem(@NotNull String key, @NotNull PhpNamedElement phpNamedElement) {
        @Override
        public String toString() {
            return key;
        }
    }
}
