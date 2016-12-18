package fr.adrienbrault.idea.symfony2plugin.action.bundle;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.AbstractProjectDumbAwareAction;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;

public class WebTestCaseGeneratorAction extends AbstractProjectDumbAwareAction {

    public WebTestCaseGeneratorAction() {
        super("Create WebTestCase", "Create WebTestCase class", Symfony2Icons.SYMFONY);
    }

    public void update(AnActionEvent event) {
        Project project = getEventProject(event);
        if(project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            this.setStatus(event, false);
            return;
        }

        this.setStatus(event, BundleClassGeneratorUtil.getBundleDirContext(event) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = getEventProject(event);
        if(project == null) {
            return;
        }

        PsiDirectory bundleDirContext = BundleClassGeneratorUtil.getBundleDirContext(event);
        if(bundleDirContext == null) {
            return;
        }

        PsiFile data = CommonDataKeys.PSI_FILE.getData(event.getDataContext());
        if(!(data instanceof PhpFile)) {
            return;
        }

        String relativePath = VfsUtil.getRelativePath(data.getVirtualFile(), bundleDirContext.getVirtualFile());
        if(relativePath == null) {
            return;
        }

        PhpClass aClass = PhpPsiUtil.findClass((PhpFile) data, Conditions.alwaysTrue());
        if(aClass == null) {
            return;
        }

        int i = relativePath.lastIndexOf(".");
        final String className = "Tests\\" + relativePath.substring(0, i).replace("/", "\\") + "Test";

        final PhpClass phpClass = BundleClassGeneratorUtil.getBundleClassInDirectory(bundleDirContext);
        if(phpClass == null) {
            return;
        }


        new WriteCommandAction(project) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                PsiElement file = null;
                try {
                    file = PhpBundleFileFactory.createBundleFile(phpClass, "web_test_case", className, new HashMap<>());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error:" + e.getMessage());
                }

                if(file != null) {
                    new OpenFileDescriptor(getProject(), file.getContainingFile().getVirtualFile(), 0).navigate(true);
                }
            }

            @Override
            public String getGroupID() {
                return "Create Symfony WebTestFile";
            }
        }.execute();

    }

}
