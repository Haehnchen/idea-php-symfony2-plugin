package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
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
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateCreateByNameLocalQuickFix extends IntentionAndQuickFixAction implements HighPriorityAction {
    private final @NotNull String[] templateNames;

    public TemplateCreateByNameLocalQuickFix(@NotNull String... templateNames) {
        this.templateNames = templateNames;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
        return IntentionPreviewInfo.EMPTY;
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
        Collection<String> templatePaths = new LinkedHashSet<>();

        for (String templateName : templateNames) {
            templatePaths.addAll(TwigUtil.getCreateAbleTemplatePaths(project, templateName));
        }

        if(templatePaths.size() == 0) {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

            // notify error if editor is focused
            if (editor != null) {
                IdeHelper.showErrorHintIfAvailable(editor, "Can not find a target dir");
            }

            return;
        }

        List<String> list = new ArrayList<>(templatePaths);
        list.sort(new ProjectTemplateComparator());

        JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(list)
            .setTitle("Twig: Template Path")
            .setItemChosenCallback(selectedValue -> {
                String commandName = "Create Template: " + (selectedValue.length() > 15 ? selectedValue.substring(selectedValue.length() - 15) : selectedValue);
                WriteCommandAction.runWriteCommandAction(project, commandName, null, () -> createFile(project, selectedValue));
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
            relativeBlockScopeFile = VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), relativePath.substring(0, i).split("/"));
        }

        String content = TwigUtil.buildStringFromTwigCreateContainer(project, relativeBlockScopeFile);

        IdeHelper.RunnableCreateAndOpenFile runnableCreateAndOpenFile = IdeHelper.getRunnableCreateAndOpenFile(project, TwigFileType.INSTANCE, ProjectUtil.getProjectDir(project), relativePath);
        if(content != null) {
            runnableCreateAndOpenFile.setContent(content);
        }

        runnableCreateAndOpenFile.run();
    }

    private static class ProjectTemplateComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            int[] weights = new int[]{0, 0};

            int index = 0;
            for (String s : Arrays.asList(o1, o2)) {
                int weight = 0;

                // low priority for vendor and symfony
                if (s.toLowerCase().contains("/vendor/")) {
                    weight += 2;
                }

                if (s.toLowerCase().contains("/symfony/")) {
                    weight += 1;
                }

                // project finds
                if (s.toLowerCase().contains("/templates/")) {
                    weight -= 4;
                }

                // foobundle is better then any other
                if (s.toLowerCase().contains("bundle/")) {
                    weight -= 3;
                }

                weights[index] = weight;
                index += 1;
            }

            return weights[0] - weights[1];
        }
    }
}