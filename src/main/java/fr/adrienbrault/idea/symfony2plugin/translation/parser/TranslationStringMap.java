package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.PhpCatalogueParser.ArrayEntry;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.PhpCatalogueParser.ArrayNode;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.PhpCatalogueParser.NewNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

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
    public static TranslationStringMap create(@NotNull Collection<File> translationDirectories) {
        List<FileData> perFileData = translationDirectories.stream()
            .flatMap((Function<File, Stream<File>>) path -> {
                File[] found = path.listFiles((directory, s) -> isCatalogueFile(s));
                return found != null ? Arrays.stream(found) : Stream.empty();
            })
            .parallel()
            .map(FileData::parse)
            .toList();

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
     *
     * Parses the Symfony-generated catalogue PHP format using {@link PhpCatalogueParser}.
     * No PHP PSI or IntelliJ read action needed — the files are machine-generated with a consistent
     * structure: new MessageCatalogue('locale', ['domain' => ['key' => 'value', ...], ...])
     */
    private record FileData(@NotNull Map<String, Set<String>> domainMap) {
        @NotNull
        static FileData parse(@NotNull File file) {
            VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, false);
            if (virtualFile == null) {
                return empty();
            }

            String content;
            try {
                content = VfsUtil.loadText(virtualFile);
            } catch (IOException e) {
                return empty();
            }

            Map<String, Set<String>> data = new HashMap<>();

            // A compiled catalogue file may contain multiple MessageCatalogue instances,
            // e.g. the primary locale and its fallback chain appended via addFallbackCatalogue().
            for (NewNode call : PhpCatalogueParser.findNewExpressions(content)) {
                if (!call.className().endsWith("MessageCatalogue")) {
                    continue;
                }

                // new MessageCatalogue($locale, $messages) — we only need the second argument
                if (call.args().size() < 2) {
                    continue;
                }
                if (!(call.args().get(1) instanceof ArrayNode(var domainEntries))) {
                    continue;
                }

                for (ArrayEntry domainEntry : domainEntries) {
                    // each top-level entry is: domain name => array of translation keys
                    if (!(domainEntry.value() instanceof ArrayNode(var keyEntries))) {
                        continue;
                    }

                    // Symfony appends "+intl-icu" to the domain when the ICU message formatter
                    // is active (e.g. "messages+intl-icu"). Strip the suffix so completions work
                    // against the base domain name ("messages") that developers reference in code.
                    String domain = domainEntry.key().endsWith("+intl-icu")
                        ? domainEntry.key().substring(0, domainEntry.key().length() - 9)
                        : domainEntry.key();
                    if (domain.isBlank()) {
                        continue;
                    }

                    data.putIfAbsent(domain, new HashSet<>());
                    for (ArrayEntry keyEntry : keyEntries) {
                        if (keyEntry.key().isBlank()) {
                            continue;
                        }
                        data.get(domain).add(keyEntry.key());
                    }
                }
            }

            if (data.isEmpty()) {
                return empty();
            }

            Map<String, Set<String>> result = new HashMap<>(data.size());
            data.forEach((k, v) -> result.put(k, Set.copyOf(v)));
            return new FileData(Map.copyOf(result));
        }

        @NotNull
        static FileData empty() {
            return new FileData(Map.of());
        }
    }
}
