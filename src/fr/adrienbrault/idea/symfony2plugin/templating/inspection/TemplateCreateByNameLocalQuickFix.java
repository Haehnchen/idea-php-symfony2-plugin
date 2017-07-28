package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBList;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateCreateByNameLocalQuickFix extends IntentionAndQuickFixAction {
    @NotNull
    private final String templateName;

    public TemplateCreateByNameLocalQuickFix(@NotNull String templateName) {
        this.templateName = templateName;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return "Twig: Create Template";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Twig";
    }

    @Override
    public void applyFix(@NotNull Project project, PsiFile psiFile, @Nullable Editor editor) {
        applyFix(project);
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        applyFix(project);
    }

    private void applyFix(@NotNull Project project) {
        Collection<String> templatePaths = TwigUtil.getCreateAbleTemplatePaths(project, templateName);

        if(templatePaths.size() == 0) {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

            // notify error if editor is focused
            if(editor != null) {
                HintManager.getInstance().showErrorHint(editor, "Can not find a target dir");
            }

            return;
        }

        JBList<String> list = new JBList<>(templatePaths);

        JBPopup popup = JBPopupFactory.getInstance().createListPopupBuilder(list)
            .setTitle("Twig: Template Path")
            .setItemChoosenCallback(() -> {
                final String selectedValue = list.getSelectedValue();
                String commandName = "Create Template: " + (selectedValue.length() > 15 ? selectedValue.substring(selectedValue.length() - 15) : selectedValue);

                new WriteCommandAction.Simple(project, commandName) {
                    @Override
                    protected void run() {
                        createFile(project, selectedValue);
                    }
                }.execute();
            })
            .createPopup();

        // show popup in scope
        Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if(selectedTextEditor != null) {
            popup.showInBestPositionFor(selectedTextEditor);
        } else {
            popup.showCenteredInCurrentWindow(project);
        }
    }

    private static void createFile(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile relativeBlockScopeFile = null;

        int i = relativePath.lastIndexOf("/");
        if(i > 0) {
            relativeBlockScopeFile = VfsUtil.findRelativeFile(project.getBaseDir(), relativePath.substring(0, i).split("/"));
        }

        String content = TwigUtil.buildStringFromTwigCreateContainer(project, relativeBlockScopeFile);

        IdeHelper.RunnableCreateAndOpenFile runnableCreateAndOpenFile = IdeHelper.getRunnableCreateAndOpenFile(project, TwigFileType.INSTANCE, project.getBaseDir(), relativePath);
        if(content != null) {
            runnableCreateAndOpenFile.setContent(content);
        }

        runnableCreateAndOpenFile.run();
    }
}