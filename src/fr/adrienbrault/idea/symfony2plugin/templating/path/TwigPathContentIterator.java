package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;

import java.util.HashMap;
import java.util.Map;

public class TwigPathContentIterator implements ContentIterator {

    private TwigPath twigPath;
    private Project project;

    private Map<String, TwigFile> results = new HashMap<String, TwigFile>();

    public TwigPathContentIterator(Project project, TwigPath twigPath) {
        this.twigPath = twigPath;
        this.project = project;

    }

    public boolean processFile(VirtualFile virtualFile) {
        if(!(virtualFile.getFileType() instanceof TwigFileType)) {
            return true;
        }

        VirtualFile virtualDirectoryFile = twigPath.getDirectory(this.project);
        if(virtualDirectoryFile == null) {
            return true;
        }

        String templatePath = VfsUtil.getRelativePath(virtualFile, virtualDirectoryFile, '/');
        if(templatePath == null) {
            return true;
        }

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

        String namespace = this.twigPath.getNamespace().equals(TwigPathIndex.MAIN) ? "" : this.twigPath.getNamespace();

        String templateFinalName;
        if(this.twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.BUNDLE) {
            templateFinalName = namespace + ":" + templateDirectory + ":" + templateFile;
        } else {
            templateFinalName = namespace + "/" + templateDirectory + "/" + templateFile;

            // remove empty path and check for root (global namespace)
            templateFinalName = templateFinalName.replace("//", "/");
            if(templateFinalName.startsWith("/")) {
                templateFinalName = templateFinalName.substring(1);
            } else {
                templateFinalName = "@" + templateFinalName;
            }
        }


        TwigFile twigFile = (TwigFile) PsiManager.getInstance(this.project).findFile(virtualFile);
        this.results.put(templateFinalName, twigFile);

        return true;
    }

    public Map<String, TwigFile> getResults() {
        return results;
    }
}
