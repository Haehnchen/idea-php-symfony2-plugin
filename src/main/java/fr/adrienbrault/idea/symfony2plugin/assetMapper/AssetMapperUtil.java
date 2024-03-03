package fr.adrienbrault.idea.symfony2plugin.assetMapper;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.AssetMapperModule;
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.MappingFileEnum;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetMapperUtil {

    private static final Key<CachedValue<List<AssetMapperModule>>> MAPPING_CACHE = new Key<>("SYMFONY_ASSET_MAPPER_MAPPING_CACHE");

    public static List<AssetMapperModule> getMappingFiles(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            MAPPING_CACHE,
            () -> CachedValueProvider.Result.create(getMappingFilesInner(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    private static List<AssetMapperModule> getMappingFilesInner(@NotNull Project project) {
        List<AssetMapperModule> modules = new ArrayList<>();

        Set<VirtualFile> files = new LinkedHashSet<>();

        VirtualFile importmapFile = VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "importmap.php");
        if (importmapFile != null) {
            files.add(importmapFile);
        }

        VirtualFile installedFile = VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "assets", "vendor", "installed.php");
        if (installedFile != null) {
            files.add(installedFile);
        }

        files.addAll(FilenameIndex.getVirtualFilesByName("importmap.php", GlobalSearchScope.allScope(project)));
        for (VirtualFile file : FilenameIndex.getVirtualFilesByName("installed.php", GlobalSearchScope.allScope(project))) {
            // composer
            VirtualFile parent = file.getParent();
            if (parent != null && "composer".equals(parent.getName())) {
                continue;
            }

            files.add(file);
        }

        for (VirtualFile file : files) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                continue;
            }

            for (PhpReturn phpReturn : PsiTreeUtil.collectElementsOfType(psiFile, PhpReturn.class)) {
                PsiElement argument = phpReturn.getArgument();
                if (argument instanceof ArrayCreationExpression arrayCreationExpression) {
                    for (Map.Entry<String, PsiElement> entry : PhpElementsUtil.getArrayKeyValueMapWithValueAsPsiElement(arrayCreationExpression).entrySet()) {
                        String path = null;
                        String url = null;
                        String version = null;
                        Boolean entrypoint = false;
                        String type = null;

                        PsiElement value = entry.getValue();
                        if (value instanceof ArrayCreationExpression value2) {
                            path = PhpElementsUtil.getArrayValueString(value2, "path");
                            url = PhpElementsUtil.getArrayValueString(value2, "url");
                            version = PhpElementsUtil.getArrayValueString(value2, "version");
                            entrypoint = PhpElementsUtil.getArrayValueBool(value2, "entrypoint");
                            type = PhpElementsUtil.getArrayValueString(value2, "type");
                        }

                        modules.add(new AssetMapperModule(MappingFileEnum.fromString(file.getName()), file, entry.getKey(), path, url, version, entrypoint, type));
                    }
                }
            }
        }

        return modules;
    }

    @NotNull
    private static Collection<VirtualFile> getModuleReferences(@NotNull String module, @NotNull List<AssetMapperModule> modules) {
        Collection<VirtualFile> files = new HashSet<>();

        for (AssetMapperModule mappingFile : modules) {
            if (!mappingFile.key().equals(module)) {
                continue;
            }

            if (mappingFile.sourceType() == MappingFileEnum.IMPORTMAP) {
                // default project structure: "assets/vendor/*"
                VirtualFile parent = mappingFile.sourceFile().getParent();
                if (parent != null) {
                    if (mappingFile.path() != null) {
                        // simple path normalize: "./app.js"
                        String[] split = Arrays.stream(StringUtils.split(mappingFile.path(), "/"))
                            .filter(s -> !s.equals("."))
                            .toArray(String[]::new);

                        VirtualFile relativeFile = VfsUtil.findRelativeFile(parent, split);
                        if (relativeFile != null) {
                            files.add(relativeFile);
                            break;
                        }
                    } else {
                        if ("css".equals(mappingFile.type())) {
                            String[] split = StringUtils.split("assets/vendor/" + mappingFile.key(), "/");
                            VirtualFile relativeFile = VfsUtil.findRelativeFile(parent, split);
                            if (relativeFile != null) {
                                files.add(relativeFile);
                                break;
                            }
                        } else if (mappingFile.key().contains("/")) {
                            String path;
                            if (mappingFile.key().startsWith("@") && mappingFile.key().split("/").length == 2) {
                                path = mappingFile.key() + "/" +  mappingFile.key().split("/")[1] + ".index.js";
                            } else {
                                path = mappingFile.key() + ".js";
                            }

                            String[] split = StringUtils.split("assets/vendor/" + path, "/");
                            VirtualFile relativeFile = VfsUtil.findRelativeFile(parent, split);
                            if (relativeFile != null) {
                                files.add(relativeFile);
                                break;
                            }
                        } else {
                            VirtualFile relativeFile = VfsUtil.findRelativeFile(parent, "assets", "vendor", mappingFile.key(), mappingFile.key() + ".index.js");
                            if (relativeFile != null) {
                                files.add(relativeFile);
                                break;
                            }
                        }
                    }
                }
            } else if (mappingFile.sourceType() == MappingFileEnum.INSTALLED) {
                // fallback without project structure: every folder like "vendor/installed.php" => "vendor/bootstrap"
                VirtualFile parent = mappingFile.sourceFile().getParent();
                if (parent != null) {
                    if (mappingFile.key().endsWith("css")) {
                        String[] split = StringUtils.split(mappingFile.key(), "/");
                        VirtualFile relativeFile = VfsUtil.findRelativeFile(parent, split);
                        if (relativeFile != null) {
                            files.add(relativeFile);
                            break;
                        }
                    } else if (mappingFile.key().contains("/")) {
                        String path;
                        if (mappingFile.key().startsWith("@") && mappingFile.key().split("/").length == 2) {
                            path = mappingFile.key() + "/" +  mappingFile.key().split("/")[1] + ".index.js";
                        } else {
                            path = mappingFile.key() + ".js";
                        }

                        String[] split = StringUtils.split(path, "/");
                        VirtualFile relativeFile = VfsUtil.findRelativeFile(parent, split);
                        if (relativeFile != null) {
                            files.add(relativeFile);
                            break;
                        }
                    } else {
                        VirtualFile relativeFile = VfsUtil.findRelativeFile(parent, mappingFile.key(), mappingFile.key() + ".index.js");
                        if (relativeFile != null) {
                            files.add(relativeFile);
                            break;
                        }
                    }
                }
            }
        }

        return files;
    }

    @NotNull
    public static Collection<VirtualFile> getModuleReferences(@NotNull Project project, @NotNull String module) {
        return getModuleReferences(module, AssetMapperUtil.getMappingFiles(project));
    }

    @NotNull
    public static Collection<VirtualFile> getEntrypointModuleReferences(@NotNull Project project, @NotNull String module) {
        List<AssetMapperModule> collect = getEntrypointMappings(project).stream().filter(module1 -> module1.key().equals(module)).collect(Collectors.toList());

        // mapping targets
        Collection<VirtualFile> files = collect.stream()
            .map(AssetMapperModule::sourceFile)
            .collect(Collectors.toSet());

        // mapping reference tags
        files.addAll(getModuleReferences(module, collect));

        return files;
    }

    @NotNull
    private static List<AssetMapperModule> getEntrypointMappings(@NotNull Project project) {
        return AssetMapperUtil.getMappingFiles(project).stream().filter(m -> m.entrypoint() != null && m.entrypoint()).collect(Collectors.toList());
    }

    @NotNull
    public static Collection<LookupElement> getLookupElements(@NotNull Project project) {
        return getLookupElements(AssetMapperUtil.getMappingFiles(project));
    }

    @NotNull
    public static Collection<LookupElement> getEntrypointLookupElements(@NotNull Project project) {
        return getLookupElements(getEntrypointMappings(project));
    }

    @NotNull
    public static Collection<LookupElement> getLookupElements(@NotNull List<AssetMapperModule> modules) {
        Collection<LookupElement> lookupElements = new ArrayList<>();

        Set<String> visited = new HashSet<>();
        for (AssetMapperModule module : modules) {
            if (visited.contains(module.key())) {
                continue;
            }

            visited.add(module.key());

            LookupElementBuilder elementBuilder = LookupElementBuilder.create(module.key()).withIcon(Symfony2Icons.SYMFONY);
            String typeText = "";

            if (module.url() != null) {
                typeText = module.url();
            } else if (module.path() != null) {
                typeText = module.path();
            }

            if (module.version() != null) {
                if (!typeText.isBlank()) {
                    typeText = module.version() + " " + typeText;
                } else {
                    typeText = module.version();
                }
            }

            if (!typeText.isBlank()) {
                elementBuilder = elementBuilder.withTypeText(typeText);
            }

            lookupElements.add(elementBuilder);
        }

        return lookupElements;
    }
}
