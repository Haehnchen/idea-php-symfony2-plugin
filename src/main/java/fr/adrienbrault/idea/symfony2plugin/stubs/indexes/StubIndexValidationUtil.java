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

    /**
     * Case-insensitive `String.contains()`: locates the first character with the vectorized `String.indexOf()`,
     * which is several times faster on whole file contents than a case-insensitive `StringSearcher`.
     */
    public static boolean containsIgnoreCase(@NotNull String text, @NotNull String needle) {
        if (needle.isEmpty()) {
            return true;
        }

        char first = needle.charAt(0);
        char lower = Character.toLowerCase(first);
        char upper = Character.toUpperCase(first);

        return containsIgnoreCaseFrom(text, needle, lower) || (upper != lower && containsIgnoreCaseFrom(text, needle, upper));
    }

    private static boolean containsIgnoreCaseFrom(@NotNull String text, @NotNull String needle, char first) {
        int index = -1;
        while ((index = text.indexOf(first, index + 1)) >= 0) {
            if (text.regionMatches(true, index, needle, 0, needle.length())) {
                return true;
            }
        }

        return false;
    }
}
