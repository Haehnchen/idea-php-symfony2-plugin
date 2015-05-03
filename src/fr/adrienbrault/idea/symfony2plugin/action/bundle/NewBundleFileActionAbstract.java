package fr.adrienbrault.idea.symfony2plugin.action.bundle;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.refactoring.PhpNameUtil;
import fr.adrienbrault.idea.symfony2plugin.action.AbstractProjectDumbAwareAction;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class NewBundleFileActionAbstract extends AbstractProjectDumbAwareAction {

    public NewBundleFileActionAbstract(String text, String description, Icon phpFile) {
        super(text, description, phpFile);
    }

    public void update(AnActionEvent event) {
        Project project = getEventProject(event);
        if(project == null) {
            this.setStatus(event, false);
            return;
        }

        this.setStatus(event, BundleClassGeneratorUtil.getBundleDirContext(event) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        final Project project = getEventProject(event);
        if(project == null) {
            this.setStatus(event, false);
            return;
        }

        PsiDirectory bundleDirContext = BundleClassGeneratorUtil.getBundleDirContext(event);
        if(bundleDirContext == null) {
            return;
        }

        final PhpClass phpClass = BundleClassGeneratorUtil.getBundleClassInDirectory(bundleDirContext);
        if(phpClass == null) {
            return;
        }

        final String className = JOptionPane.showInputDialog("New class name:");
        if(StringUtils.isBlank(className)) {
            return;
        }

        if(!PhpNameUtil.isValidClassName(className)) {
            JOptionPane.showMessageDialog(null, "Invalid class name");
            return;
        }

        write(project, phpClass, className);
    }

    abstract protected void write(@NotNull Project project,@NotNull PhpClass phpClass, @NotNull String className);
}
