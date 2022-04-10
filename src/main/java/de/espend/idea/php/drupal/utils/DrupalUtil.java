package de.espend.idea.php.drupal.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DrupalUtil {

    @NotNull
    public static Set<String> getModuleNames(@NotNull Project project) {
        Set<String> allFilesByExt = new HashSet<>();

        for (VirtualFile virtualFile : FilenameIndex.getAllFilesByExt(project, "yml")) {
            if(!virtualFile.getName().endsWith(".info.yml")) {
                continue;
            }

            allFilesByExt.add(StringUtils.stripEnd(virtualFile.getName(), ".info.yml"));
        }

        allFilesByExt.addAll(FilenameIndex.getAllFilesByExt(project, "module").stream()
            .map(virtualFile -> StringUtils.stripEnd(virtualFile.getName(), ".module"))
            .collect(Collectors.toList()));

        return allFilesByExt;
    }

}
