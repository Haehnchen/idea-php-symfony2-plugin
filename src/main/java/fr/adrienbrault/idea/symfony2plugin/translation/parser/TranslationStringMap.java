package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationStringMap {
    @NotNull
    private final Map<String, Set<String>> domainMap;

    private TranslationStringMap(@NotNull Map<String, Set<String>> domainMap) {
        this.domainMap = domainMap;
    }

    @Nullable
    public Set<String> getDomainMap(@NotNull String domainKey) {
        return domainMap.get(domainKey);
    }

    @NotNull
    public Set<String> getDomainList() {
        return domainMap.keySet();
    }

    public static boolean isCatalogueFile(@NotNull String filename) {
        return filename.startsWith("catalogue") && filename.endsWith(".php");
    }

    @NotNull
    public static TranslationStringMap createEmpty() {
        return new TranslationStringMap(Map.of());
    }

    @NotNull
    public static TranslationStringMap create(@NotNull Project project, @NotNull Collection<File> translationDirectories) {
        List<FileData> perFileData = new ArrayList<>();

        for (File path : translationDirectories) {
            File[] files = path.listFiles((directory, s) -> isCatalogueFile(s));
            if (files == null) {
                continue;
            }

            for (File fileEntry : files) {
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(fileEntry, false);
                if (virtualFile != null) {
                    perFileData.add(FileData.parse(project, virtualFile));
                } else {
                    Symfony2ProjectComponent.getLogger().info("VfsUtil missing translation: " + fileEntry.getPath());
                }
            }
        }

        return merge(perFileData);
    }

    @NotNull
    private static TranslationStringMap merge(@NotNull Collection<FileData> perFileData) {
        Map<String, Set<String>> merged = new HashMap<>();

        for (FileData fileData : perFileData) {
            fileData.domainMap().forEach((domain, keys) -> merged.computeIfAbsent(domain, k -> new HashSet<>()).addAll(keys));
        }

        Map<String, Set<String>> result = new HashMap<>(merged.size());
        merged.forEach((k, v) -> result.put(k, Set.copyOf(v)));

        return new TranslationStringMap(Map.copyOf(result));
    }

    /**
     * Immutable parsed data for a single catalogue file.
     * The unit of per-file caching: parse once per VirtualFile, then merge via {@link #merge}.
     */
    private record FileData(@NotNull Map<String, Set<String>> domainMap) {
        @NotNull
        public static FileData parse(@NotNull Project project, @NotNull VirtualFile virtualFile) {
            PsiFile psiFile;
            try {
                String content = VfsUtil.loadText(virtualFile);
                psiFile = PhpPsiElementFactory.createPsiFileFromText(project, content);
            } catch (IOException e) {
                return empty();
            }

            if (psiFile == null) {
                return empty();
            }

            Symfony2ProjectComponent.getLogger().info("update translations: " + virtualFile.getPath());

            // local mutable accumulator — never escapes this method
            Map<String, Set<String>> data = new HashMap<>();

            for (NewExpression newExpression : PsiTreeUtil.collectElementsOfType(psiFile, NewExpression.class)) {
                ClassReference classReference = newExpression.getClassReference();
                if (classReference == null) {
                    continue;
                }
                if ("\\Symfony\\Component\\Translation\\MessageCatalogue".equals(classReference.getFQN())) {
                    collectMessages(newExpression, data);
                }
            }

            Map<String, Set<String>> result = new HashMap<>(data.size());
            data.forEach((k, v) -> result.put(k, Set.copyOf(v)));
            return new FileData(Map.copyOf(result));
        }

        @NotNull
        static FileData empty() {
            return new FileData(Map.of());
        }

        private static void collectMessages(@NotNull NewExpression newExpression, @NotNull Map<String, Set<String>> data) {
            PsiElement[] parameters = newExpression.getParameters();
            if (parameters.length < 2 || !(parameters[1] instanceof ArrayCreationExpression)) {
                return;
            }

            for (ArrayHashElement arrayHashElement : PsiTreeUtil.getChildrenOfTypeAsList(parameters[1], ArrayHashElement.class)) {
                PhpPsiElement arrayKey = arrayHashElement.getKey();
                if (!(arrayKey instanceof StringLiteralExpression)) {
                    continue;
                }

                String transDomain = ((StringLiteralExpression) arrayKey).getContents();
                if (StringUtils.isBlank(transDomain)) {
                    continue;
                }

                if (transDomain.endsWith("+intl-icu")) {
                    transDomain = transDomain.substring(0, transDomain.length() - 9);
                }

                if (StringUtils.isBlank(transDomain)) {
                    continue;
                }

                data.putIfAbsent(transDomain, new HashSet<>());

                PhpPsiElement arrayValue = arrayHashElement.getValue();
                if (arrayValue instanceof ArrayCreationExpression) {
                    collectKeys(transDomain, (ArrayCreationExpression) arrayValue, data);
                }
            }
        }

        private static void collectKeys(@NotNull String domain, @NotNull ArrayCreationExpression translationArray, @NotNull Map<String, Set<String>> data) {
            for (ArrayHashElement arrayHashElement : PsiTreeUtil.getChildrenOfTypeAsList(translationArray, ArrayHashElement.class)) {
                PhpPsiElement translationKey = arrayHashElement.getKey();
                if (translationKey instanceof StringLiteralExpression) {
                    data.computeIfAbsent(domain, k -> new HashSet<>()).add(((StringLiteralExpression) translationKey).getContents());
                }
            }
        }
    }
}
