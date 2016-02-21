package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.ui.TemplateCreationSelectionDialog;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


public class PhpTemplateAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(element.getProject()) || !Settings.getInstance(element.getProject()).phpAnnotateTemplate) {
            return;
        }


        MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element);
        if (methodReference == null || !new Symfony2InterfacesUtil().isTemplatingRenderCall(methodReference)) {
            return;
        }

        ParameterBag parameterBag = PsiElementUtils.getCurrentParameterIndex(element.getParent());
        if(parameterBag == null || parameterBag.getIndex() != 0) {
            return;
        }


        String templateName = Symfony2InterfacesUtil.getFirstArgumentStringValue(methodReference);
        if(templateName == null || StringUtils.isBlank(templateName)) {
            return;
        }

        if(TwigHelper.getTemplatePsiElements(element.getProject(), templateName).length > 0)  {
           return;
        }

        holder.createWarningAnnotation(element, "Missing Template");

        holder.createWarningAnnotation(element, "Create Template")
            .registerFix(new CreateTemplateFix(templateName));

    }

    public static class CreateTemplateFix extends BaseIntentionAction {

        private String templateName;
        public CreateTemplateFix(String templateName) {
            this.templateName = templateName;
        }

        @NotNull
        @Override
        public String getText() {
            return "Create template";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Symfony";
        }

        @Override
        public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
            return true;
        }

        @Override
        public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {

                    Collection<String> templatePaths = TwigUtil.getCreateAbleTemplatePaths(project, templateName);
                    if(templatePaths.size() == 0) {
                        HintManager.getInstance().showErrorHint(editor, "Can not find a target dir");
                        return;
                    }

                    if(templatePaths.size() == 1) {
                        createFile(templatePaths.iterator().next());
                        return;
                    }

                    TemplateCreationSelectionDialog.create(templatePaths, new TemplateCreationSelectionDialog.Callback() {
                        @Override
                        public void ok(@NotNull String selected) {
                            createFile(selected);
                        }
                    });
                }

                private void createFile(@NotNull  String relativePath) {
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

                    ApplicationManager.getApplication().runWriteAction(runnableCreateAndOpenFile);
                }
            });
        }
    }


}