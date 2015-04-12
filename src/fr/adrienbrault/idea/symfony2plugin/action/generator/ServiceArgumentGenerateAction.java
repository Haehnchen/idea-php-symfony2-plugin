package fr.adrienbrault.idea.symfony2plugin.action.generator;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.intentions.php.XmlServiceArgumentIntention;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ServiceArgumentGenerateAction extends CodeInsightAction {

    @Override
    public void update(AnActionEvent event) {
        super.update(event);
        event.getPresentation().setVisible(Symfony2ProjectComponent.isEnabled(event.getProject()));
    }

    @Nullable
    private static XmlTag getMatchXmlTag(@NotNull Editor editor, @NotNull PsiFile file) {

        if(!(file instanceof XmlFile)) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        if(offset <= 0) {
            return null;
        }

        PsiElement psiElement = file.findElementAt(offset);
        if(psiElement == null) {
            return null;
        }

        return XmlServiceArgumentIntention.getServiceTagValid(psiElement);
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {

        if(file.getFileType() != XmlFileType.INSTANCE || !Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        return getMatchXmlTag(editor, file) != null;
    }
    
    
    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {

        return new CodeInsightActionHandler() {
            @Override
            public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {

                XmlTag serviceTag = getMatchXmlTag(editor, psiFile);
                if(serviceTag == null) {
                    return;
                }

                if(!ServiceActionUtil.isValidXmlParameterInspectionService(serviceTag)) {
                    HintManager.getInstance().showErrorHint(editor, "Sry, not supported service definition");
                    return;
                }

                List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(serviceTag, new ContainerCollectionResolver.LazyServiceCollector(project));
                if (args == null) {
                    return;
                }

                ServiceActionUtil.fixServiceArgument(args, serviceTag);
            }

            @Override
            public boolean startInWriteAction() {
                return true;
            }
        };

    }

}
