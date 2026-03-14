package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceParserInterface {
    String getXPathFilter();

    /**
     * Parse service data from input stream with source file context.
     * @param inputStream the input stream to parse
     * @param sourceFile the VirtualFile being parsed, or null if not available
     * @param project the current IntelliJ project, or null if not available
     */
    void parser(@NotNull InputStream inputStream, @Nullable VirtualFile sourceFile, @Nullable Project project);
}
