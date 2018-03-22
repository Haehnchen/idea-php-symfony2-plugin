package fr.adrienbrault.idea.symfony2plugin.action.bundle;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewBundleCommandAction extends NewBundleFileActionAbstract {

    public NewBundleCommandAction() {
        super("Command", "Create Command Class", Symfony2Icons.SYMFONY);
    }

    protected void write(@NotNull final Project project, @NotNull final PhpClass phpClass, @NotNull String className) {

        if(!className.endsWith("Command")) {
            className += "Command";
        }

        final String finalClassName = className;
        new WriteCommandAction(project) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {

                PsiElement bundleFile = null;
                try {

                    bundleFile = PhpBundleFileFactory.createBundleFile(phpClass, "command", "Command\\" + finalClassName, new HashMap<String, String>() {{
                        String name = phpClass.getName();
                        if(name.endsWith("Bundle")) {
                            name = name.substring(0, name.length() - "Bundle".length());
                        }
                        put("name", StringUtils.underscore(name) + ":" + StringUtils.underscore(finalClassName));
                    }});

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
                return "Create Command";
            }
        }.execute();
    }

}
