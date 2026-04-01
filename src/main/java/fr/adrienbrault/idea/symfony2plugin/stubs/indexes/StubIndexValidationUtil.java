package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.FileContent;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Shared validation rules for file-based stub indexes.
 */
final class StubIndexValidationUtil {
    private static final Set<String> EXCLUDED_PATH_PARTS = Set.of("/Test/", "/Tests/", "/Fixture/", "/Fixtures/");

    private StubIndexValidationUtil() {
    }

    /**
     * Applies the shared file-name, extension, test-path, and file-size checks
     * used by multiple stub indexes.
     */
    public static boolean isValidForIndex(
        @NotNull FileContent inputData,
        @NotNull PsiFile psiFile,
        int maxFileByteSize,
        boolean excludeTestPaths,
        @Nullable Set<String> allowedExtensions
    ) {
        String fileName = psiFile.getName();
        if (fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        if (allowedExtensions != null) {
            String extension = inputData.getFile().getExtension();
            if (extension == null || allowedExtensions.stream().noneMatch(it -> it.equalsIgnoreCase(extension))) {
                return false;
            }
        }

        if (excludeTestPaths) {
            String relativePath = VfsUtil.getRelativePath(inputData.getFile(), ProjectUtil.getProjectDir(inputData.getProject()), '/');
            if (relativePath != null && EXCLUDED_PATH_PARTS.stream().anyMatch(relativePath::contains)) {
                return false;
            }
        }

        return inputData.getFile().getLength() <= maxFileByteSize;
    }
}
