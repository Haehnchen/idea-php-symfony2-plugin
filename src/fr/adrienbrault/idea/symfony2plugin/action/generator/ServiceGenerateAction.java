package fr.adrienbrault.idea.symfony2plugin.action.generator;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServiceGenerateAction extends CodeInsightAction {

    @Override
    public void update(AnActionEvent event) {
        super.update(event);
        event.getPresentation().setVisible(Symfony2ProjectComponent.isEnabled(event.getProject()));
    }

    public static void invokeServiceGenerator(@NotNull Project project, @NotNull PsiFile file, @NotNull PhpClass phpClass, @Nullable Editor editor) {
        SymfonyCreateService.create(project, file, phpClass, editor);
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return isValidForPhpClass(editor, file) || isValidForXml(editor, file);
    }

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            @Override
            public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {

                if(invokePhpClass(project, editor)) {
                    return;
                }

                if(isValidForXml(editor, psiFile) && invokeXmlFile(project, editor)) {
                    return;
                }

            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    private boolean isValidForPhpClass(Editor editor, PsiFile file) {

        if(!(file instanceof PhpFile)) {
            return false;
        }

        int offset = editor.getCaretModel().getOffset();
        if(offset <= 0) {
            return false;
        }

        PsiElement psiElement = file.findElementAt(offset);
        if(psiElement == null) {
            return false;
        }

        if(!PlatformPatterns.psiElement().inside(PhpClass.class).accepts(psiElement)) {
            return false;
        }

        return true;
    }

    private boolean isValidForXml(Editor editor, PsiFile file) {

        if(!(file instanceof XmlFile)) {
            return false;
        }

        XmlTag rootTag = ((XmlFile) file).getRootTag();
        if(rootTag == null || !"container".equals(rootTag.getName())) {
            return false;
        }

        return true;
    }

    private boolean invokeXmlFile(Project project, Editor editor) {

        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if(file == null) {
            return false;
        }

        SymfonyCreateService.create(project, file, editor);

        return true;
    }

    private boolean invokePhpClass(Project project, Editor editor) {

        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if(file == null) {
            return false;
        }

        int offset = editor.getCaretModel().getOffset();
        if(offset <= 0) {
            return false;
        }

        PsiElement psiElement = file.findElementAt(offset);
        if(psiElement == null) {
            return false;
        }

        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if(phpClass == null) {
            return false;
        }

        invokeServiceGenerator(project, file, phpClass, editor);

        return true;
    }

}
