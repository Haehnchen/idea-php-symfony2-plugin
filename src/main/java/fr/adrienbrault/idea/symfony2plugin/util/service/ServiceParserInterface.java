package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceParserInterface {
    String getXPathFilter();

    void parser(@NotNull InputStream inputStream, @NotNull VirtualFile sourceFile, @NotNull Project project);
}
