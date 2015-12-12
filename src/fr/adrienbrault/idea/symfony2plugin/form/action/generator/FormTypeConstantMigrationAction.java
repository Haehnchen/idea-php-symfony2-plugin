package fr.adrienbrault.idea.symfony2plugin.form.action.generator;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.jetbrains.php.lang.psi.PhpCodeEditUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeConstantMigrationAction extends CodeInsightAction {

    @Override
    public void update(AnActionEvent event) {
        super.update(event);
        event.getPresentation().setVisible(Symfony2ProjectComponent.isEnabled(event.getProject()));
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        PhpClass classAtCaret = PhpCodeEditUtil.findClassAtCaret(editor, file);

        return
            classAtCaret != null &&
            new Symfony2InterfacesUtil().isInstanceOf(classAtCaret, "Symfony\\Component\\Form\\FormTypeInterface")
        ;
    }

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
            PhpClass phpClass = PhpCodeEditUtil.findClassAtCaret(editor, psiFile);
            if(phpClass == null) {
                HintManager.getInstance().showErrorHint(editor, "No class context found");
                return;
            }

            final Collection<StringLiteralExpression> formTypes = new ArrayList<StringLiteralExpression>();
            phpClass.acceptChildren(new FormTypeStringElementVisitor(formTypes));

            if(formTypes.size() == 0) {
                HintManager.getInstance().showErrorHint(editor, "Nothing to do for me");
                return;
            }

            for (StringLiteralExpression formType : formTypes) {
                try {
                    FormUtil.replaceFormStringAliasWithClassConstant(formType);
                } catch (Exception ignored) {
                }
            }

        }

        @Override
        public boolean startInWriteAction() {
            return true;
        }

        private class FormTypeStringElementVisitor extends PsiRecursiveElementVisitor {
            private final Collection<StringLiteralExpression> formTypes;

            public FormTypeStringElementVisitor(Collection<StringLiteralExpression> formTypes) {
                this.formTypes = formTypes;
            }

            @Override
            public void visitElement(PsiElement element) {
                if (!(element instanceof StringLiteralExpression)) {
                    super.visitElement(element);
                    return;
                }

                String contents = ((StringLiteralExpression) element).getContents();
                if (StringUtils.isBlank(contents)) {
                    super.visitElement(element);
                    return;
                }

                if (null == new MethodMatcher.StringParameterMatcher(element, 1)
                    .withSignature(Symfony2InterfacesUtil.getFormBuilderInterface())
                    .match()) {

                    super.visitElement(element);
                    return;
                }

                formTypes.add((StringLiteralExpression) element);

                super.visitElement(element);
            }
        }
    }
}
