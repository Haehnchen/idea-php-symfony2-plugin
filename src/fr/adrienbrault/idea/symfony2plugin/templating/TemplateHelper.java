package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateHelper {

    public static Map<String, TwigFile> getTwigFilesByName(Project project) {
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
        Collection<PhpClass> phpClasses = phpIndex.getAllSubclasses("\\Symfony\\Component\\HttpKernel\\Bundle\\Bundle");

        Map<String, PsiDirectory> bundlesDirectories = new HashMap<String, PsiDirectory>();
        for (PhpClass phpClass : phpClasses) {
            bundlesDirectories.put(phpClass.getName(), phpClass.getContainingFile().getContainingDirectory());
        }

        Collection<VirtualFile> twigVirtualFiles = fileBasedIndex.getContainingFiles(FileTypeIndex.NAME, TwigFileType.INSTANCE, GlobalSearchScope.projectScope(project));
        Map<String, TwigFile> results = new HashMap<String, TwigFile>();
        for (VirtualFile twigVirtualFile : twigVirtualFiles) {
            // Find in which bundle it is
            for (Map.Entry<String, PsiDirectory> pair : bundlesDirectories.entrySet()) {
                if (!VfsUtil.isAncestor((pair.getValue()).getVirtualFile(), twigVirtualFile, false)) {
                    continue;
                }

                String bundleName = pair.getKey(); // XXX:xxx:xxx
                String templatePath = VfsUtil.getRelativePath(twigVirtualFile, (pair.getValue()).getVirtualFile(), '/'); // Resources/views/xxx.twig
                if (null == templatePath || !templatePath.startsWith("Resources/views")) {
                    continue;
                }

                templatePath = templatePath.substring("Resources/views/".length()); // xxx.twig
                String templateDirectory = null; // xxx:XXX:xxx
                String templateFile = null; // xxx:xxx:XXX
                if (templatePath.contains("/")) {
                    int lastDirectorySeparatorIndex = templatePath.lastIndexOf("/");
                    templateDirectory = templatePath.substring(0, lastDirectorySeparatorIndex);
                    templateFile = templatePath.substring(lastDirectorySeparatorIndex + 1);
                } else {
                    templateDirectory = "";
                    templateFile = templatePath;
                }

                String templateFinalName = bundleName + ":" + templateDirectory + ":" + templateFile;
                TwigFile twigFile = (TwigFile) PsiManager.getInstance(project).findFile(twigVirtualFile);

                results.put(templateFinalName, twigFile);
            }
        }

        return results;
    }

}
