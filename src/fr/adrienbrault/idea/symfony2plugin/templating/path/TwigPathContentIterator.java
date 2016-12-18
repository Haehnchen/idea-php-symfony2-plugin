package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.twig.TwigFileType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathContentIterator {

    private final TwigPath twigPath;

    @NotNull
    private final Project project;

    @NotNull
    private Map<String, VirtualFile> results = new HashMap<>();

    private boolean withPhp = false;
    private boolean withTwig = true;

    @NotNull
    private Set<String> workedOn = new HashSet<>();

    public TwigPathContentIterator(@NotNull Project project, @NotNull TwigPath twigPath) {
        this.twigPath = twigPath;
        this.project = project;
    }

    public boolean processFile(VirtualFile virtualFile) {

        // @TODO make file types more dynamically like eg js
        if(!this.isProcessable(virtualFile)) {
            return true;
        }

        // prevent process double file if processCollection and ContentIterator is used in one instance
        String filePath = virtualFile.getPath();
        if(workedOn.contains(filePath)) {
            return true;
        }

        workedOn.add(filePath);

        VirtualFile virtualDirectoryFile = twigPath.getDirectory(this.project);
        if(virtualDirectoryFile == null) {
            return true;
        }

        String templatePath = VfsUtil.getRelativePath(virtualFile, virtualDirectoryFile, '/');
        if(templatePath == null) {
            return true;
        }

        String templateDirectory; // xxx:XXX:xxx
        String templateFile; // xxx:xxx:XXX

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

        this.results.put(templateFinalName, virtualFile);

        return true;
    }

    private boolean isProcessable(VirtualFile virtualFile) {

        if(virtualFile.isDirectory()) {
            return false;
        }

        if(withTwig && virtualFile.getFileType() instanceof TwigFileType) {
            return true;
        }

        if(withPhp && virtualFile.getFileType() instanceof PhpFileType) {
            return true;
        }

        return false;
    }

    public TwigPathContentIterator setWithPhp(boolean withPhp) {
        this.withPhp = withPhp;
        return this;
    }

    public TwigPathContentIterator setWithTwig(boolean withTwig) {
        this.withTwig = withTwig;
        return this;
    }

    @NotNull
    public Map<String, VirtualFile> getResults() {
        return results;
    }
}
