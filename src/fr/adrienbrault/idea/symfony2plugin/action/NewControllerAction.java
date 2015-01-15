package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

public class NewControllerAction extends AbstractProjectDumbAwareAction {

    public NewControllerAction() {
        super("Controller", "Create new Controller File", PhpIcons.PHP_FILE);
    }

    public void update(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);

        if (project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            this.setStatus(event, false);
            return;
        }

        DataContext dataContext = event.getDataContext();
        IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        if (view == null) {
            this.setStatus(event, false);
            return;
        }

        final PsiDirectory initialBaseDir = view.getOrChooseDirectory();
        if (initialBaseDir == null) {
            this.setStatus(event, false);
            return;
        }

        if(!initialBaseDir.getName().equals("Controller")) {
            this.setStatus(event, false);
        }

    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getData(PlatformDataKeys.PROJECT);
        buildFile(event, project, "/resources/fileTemplates/controller.php");
    }

    public static void buildFile(AnActionEvent event, final Project project, String templatePath) {

        String fileName = Messages.showInputDialog(project, "File name (without extension)", "Create Controller", PhpIcons.PHP_FILE);
        if(fileName == null || StringUtils.isBlank(fileName)) {
            return;
        }

        if(fileName.endsWith("Controller")) {
            fileName = fileName.substring(0, fileName.length() - 10);
        }

        fileName = StringUtils.capitalize(fileName);
        if(fileName == null || StringUtils.isBlank(fileName)) {
            return;
        }

        DataContext dataContext = event.getDataContext();
        IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        if (view == null) {
            return;
        }

        final PsiDirectory initialBaseDir = view.getOrChooseDirectory();
        if (initialBaseDir == null) {
            return;
        }

        String content;
        try {
            content = StreamUtil.readText(ServiceActionUtil.class.getResourceAsStream(templatePath), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final PsiFileFactory factory = PsiFileFactory.getInstance(project);

        String bundleName = "Acme\\DemoBundle";

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(project);
        SymfonyBundle symfonyBundle = symfonyBundleUtil.getContainingBundle(initialBaseDir);

        if(symfonyBundle != null) {
            bundleName = StringUtils.strip(symfonyBundle.getNamespaceName(), "\\");
            String path = symfonyBundle.getRelative(initialBaseDir.getVirtualFile());
            if(path != null) {
                bundleName = bundleName.concat("\\" + path);
            }
        }

        content = content.replace("{{ Namespace }}", bundleName).replace("{{ ControllerName }}", fileName);

        fileName = fileName.concat("Controller.php");

        if(initialBaseDir.findFile(fileName) != null) {
            Messages.showInfoMessage("File exists", "Error");
            return;
        }

        final PsiFile file = factory.createFileFromText(fileName, PhpFileType.INSTANCE, content);

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                CodeStyleManager.getInstance(project).reformat(file);
                initialBaseDir.add(file);
            }
        });

        PsiFile psiFile = initialBaseDir.findFile(fileName);
        if(psiFile != null) {
            view.selectElement(psiFile);
        }

    }
}
