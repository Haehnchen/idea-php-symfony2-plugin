package fr.adrienbrault.idea.symfony2plugin.action;


import com.intellij.ide.IdeView;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.yaml.YAMLFileType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ServiceActionUtil {

    public static void buildFile(AnActionEvent event, final Project project, String templatePath) {
        String extension = templatePath.endsWith(".yml") ? "yml" : "xml" ;

        String fileName = Messages.showInputDialog(project, "File name (without extension)", String.format("Create %s Service", extension), Symfony2Icons.SYMFONY);
        if(fileName == null || StringUtils.isBlank(fileName)) {
            return;
        }

        FileType fileType = templatePath.endsWith(".yml") ? YAMLFileType.YML : XmlFileType.INSTANCE ;

        if(!fileName.endsWith("." + extension)) {
            fileName = fileName.concat("." + extension);
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

        if(initialBaseDir.findFile(fileName) != null) {
            Messages.showInfoMessage("File exists", "Error");
            return;
        }

        String content;
        try {
            content = inputStreamToString(ServiceActionUtil.class.getResourceAsStream(templatePath));
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
        }

        String underscoreBundle = bundleName.replace("\\", ".").toLowerCase();
        if(underscoreBundle.endsWith("bundle")) {
            underscoreBundle = underscoreBundle.substring(0, underscoreBundle.length() - 6);
        }

        content = content.replace("{{ BundleName }}", bundleName).replace("{{ BundleNameUnderscore }}", underscoreBundle);

        final PsiFile file = factory.createFileFromText(fileName, fileType, content);

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

    public static String inputStreamToString(InputStream in) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;

        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }

        bufferedReader.close();
        return stringBuilder.toString();
    }
}
