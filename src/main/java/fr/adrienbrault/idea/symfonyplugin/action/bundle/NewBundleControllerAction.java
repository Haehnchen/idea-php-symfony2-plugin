package fr.adrienbrault.idea.symfonyplugin.action.bundle;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.util.psi.PhpBundleFileFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewBundleControllerAction extends NewBundleFileActionAbstract {

    public NewBundleControllerAction() {
        super("Controller", "Create new Controller File", Symfony2Icons.SYMFONY);
    }

    @Override
    protected void write(@NotNull final Project project, @NotNull final PhpClass phpClass, @NotNull String className) {

        if(!className.endsWith("Controller")) {
            className += "Controller";
        }

        final String finalClassName = className;
        new WriteCommandAction(project) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {

                PsiElement bundleFile = null;
                try {
                    bundleFile = PhpBundleFileFactory.createBundleFile(phpClass, "controller", "Controller\\" + finalClassName, new HashMap<>());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error:" + e.getMessage());
                    return;
                }

                if(bundleFile != null) {
                    new OpenFileDescriptor(getProject(), bundleFile.getContainingFile().getVirtualFile(), 0).navigate(true);
                }
            }

            @Override
            public String getGroupID() {
                return "Create Controller";
            }
        }.execute();

    }

}
