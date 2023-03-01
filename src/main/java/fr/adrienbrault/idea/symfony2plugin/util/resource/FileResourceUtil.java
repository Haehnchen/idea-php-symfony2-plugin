package fr.adrienbrault.idea.symfony2plugin.util.resource;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIcons;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResourceContextTypeEnum;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FileResourcesIndex;
import fr.adrienbrault.idea.symfony2plugin.util.FileResourceVisitorUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResourceUtil {
    /**
     * chars that trigger a glob resolving on symfony
     * extracted from: \Symfony\Component\Config\Loader\FileLoader::import
     */
    public static final String[] GLOB_DETECTION_CHARS = {"*", "?", "{", "["};

    /**
     * Search for files refers to given file
     */
    public static boolean hasFileResources(@NotNull Project project, @NotNull PsiFile psiFile) {
        return CachedValuesManager.getCachedValue(
            psiFile,
            () -> {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                if (virtualFile == null) {
                    return CachedValueProvider.Result.create(Boolean.FALSE, FileIndexCaches.getModificationTrackerForIndexId(project, FileResourcesIndex.KEY));
                }

                final Boolean[] aFalse = {Boolean.FALSE};

                visitFileResources(project, virtualFile, pair -> {
                    aFalse[0] = Boolean.TRUE;
                    return true;
                });

                return CachedValueProvider.Result.create(aFalse[0], FileIndexCaches.getModificationTrackerForIndexId(project, FileResourcesIndex.KEY));
            }
        );
    }

    /**
     * Search for files refers to given file
     */
    @NotNull
    public static Collection<Pair<VirtualFile, String>> getFileResources(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        Collection<Pair<VirtualFile, String>> files = new ArrayList<>();

        visitFileResources(project, virtualFile, pair -> {
            files.add(Pair.create(pair.getFirst(), pair.getSecond()));
            return false;
        });

        return files;
    }

    /**
     * Search for files refers to given file
     */
    public static void visitFileResources(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull Function<Pair<VirtualFile, String>, Boolean> consumer) {
        Set<VirtualFile> files = new HashSet<>();
        for (String resource : FileBasedIndex.getInstance().getAllKeys(FileResourcesIndex.KEY, project)) {
            files.addAll(FileBasedIndex.getInstance().getContainingFiles(FileResourcesIndex.KEY, resource, GlobalSearchScope.allScope(project)));
        }

        for (VirtualFile containingFile : files) {
            for (FileResource fileResourceContext : FileBasedIndex.getInstance().getFileData(FileResourcesIndex.KEY, containingFile, project).values()) {
                String resource = fileResourceContext.getResource();
                if (resource == null) {
                    continue;
                }

                VirtualFile directory = containingFile.getParent();
                if (directory == null) {
                    continue;
                }

                String resourceResolved = resource;

                if (resource.startsWith("@")) {
                    String replace = resource.replace("\\", "/");
                    int i = replace.indexOf("/");

                    boolean resolved = false;

                    if (i > 2 || i == -1) {
                        if (i == -1) {
                            i = resource.length();
                        }

                        String substring = resource.substring(1, i);
                        Collection<SymfonyBundle> bundle = new SymfonyBundleUtil(project).getBundle(substring);

                        for (SymfonyBundle symfonyBundle : bundle) {
                            PsiDirectory directory1 = symfonyBundle.getDirectory();
                            if (directory1 == null) {
                                continue;
                            }

                            resourceResolved = resource.substring(replace.contains("/") ? i + 1 : replace.length());
                            directory = directory1.getVirtualFile();

                            resolved = true;

                            break;
                        }
                    }

                    if (!resolved) {
                        continue;
                    }
                }

                // '../src/{Entity}'
                // '../src/*Controller.php'
                if (Arrays.stream(GLOB_DETECTION_CHARS).anyMatch(resource::contains)) {
                    String path = directory.getPath();

                    // nested types not support by java glob implementation so just catch the exception: "../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php,Service/{IspConfiguration,DataCollection}}"
                    try {
                        String s1 = Paths.get(path + File.separatorChar + StringUtils.stripStart(resourceResolved, "\\/")).normalize().toString();
                        String syntaxAndPattern = "glob:" + s1;
                        if (FileSystems.getDefault().getPathMatcher(syntaxAndPattern).matches(Paths.get(virtualFile.getPath()))) {
                            if (consumer.apply(new Pair<>(containingFile, resource))) {
                                return;
                            }
                        }
                    } catch (PatternSyntaxException | InvalidPathException ignored) {
                    }

                    continue;
                }

                // '../src/FooController.php'
                VirtualFile relativeFile = VfsUtil.findRelativeFile(directory, resourceResolved.replace("\\", "/").split("/"));
                if (relativeFile != null && relativeFile.equals(virtualFile)) {
                    if (consumer.apply(new Pair<>(containingFile, resource))) {
                        return;
                    }
                }

                // '..src/Controller'
                if (fileResourceContext.getContextType() == FileResourceContextTypeEnum.ROUTE) {
                    if (StringUtils.isNotBlank(resourceResolved)) {
                        directory = VfsUtil.findRelativeFile(directory, resourceResolved.replace("\\", "/").split("/"));
                        if (directory == null) {
                            continue;
                        }
                    }

                    if (VfsUtil.isAncestor(directory, virtualFile, false)) {
                        if (consumer.apply(new Pair<>(containingFile, resource))) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @Nullable
    public static RelatedItemLineMarkerInfo<PsiElement> getFileImplementsLineMarker(@NotNull PsiFile psiFile) {
        final Project project = psiFile.getProject();

        VirtualFile virtualFile = psiFile.getVirtualFile();
        if(virtualFile == null) {
            return null;
        }

        if (hasFileResources(project, psiFile)) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTS)
                .setTargets(NotNullLazyValue.lazy(new FileResourceNotNullLazyValue(project, virtualFile)))
                .setTooltipText("Navigate to resource");

            return builder.createLineMarkerInfo(psiFile);
        }

        return null;
    }

    /**
     * Gives targets to files on Bundle locate syntax. "@FooBundle/.../foo.yml"
     */
    @NotNull
    public static Collection<PsiFile> getFileResourceTargetsInBundleScope(@NotNull Project project, @NotNull String content) {

        // min validation "@FooBundle/foo.yml"
        if(!content.startsWith("@") || !content.contains("/")) {
            return Collections.emptyList();
        }

        String bundleName = content.substring(1, content.indexOf("/"));

        Collection<PsiFile> targets = new HashSet<>();

        for (SymfonyBundle bundle : new SymfonyBundleUtil(project).getBundle(bundleName)) {
            String path = content.substring(content.indexOf("/") + 1);
            PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(project, bundle.getRelative(path));

            if(psiFile != null) {
                targets.add(psiFile);
            }
        }

        return targets;
    }

    /**
     * resource: "@AppBundle/Controller/"
     */
    @NotNull
    public static Collection<PsiElement> getFileResourceTargetsInBundleDirectory(@NotNull Project project, @NotNull String content) {
        // min validation "@FooBundle/foo.yml"
        if(!content.startsWith("@")) {
            return Collections.emptyList();
        }

        content = content.replace("/", "\\");
        if(!content.contains("\\")) {
            return Collections.emptyList();
        }

        String bundleName = content.substring(1, content.indexOf("\\"));

        if(new SymfonyBundleUtil(project).getBundle(bundleName).size() == 0) {
            return Collections.emptyList();
        }

        // support double backslashes
        content = content.replaceAll("\\\\+", "\\\\");
        String namespaceName = "\\" + StringUtils.strip(content.substring(1), "\\");
        return new ArrayList<>(PhpIndexUtil.getPhpClassInsideNamespace(
            project, namespaceName
        ));
    }

    /**
     * Gives targets to files which relative to current file directory
     */
    @NotNull
    public static Collection<PsiFile> getFileResourceTargetsInDirectoryScope(@NotNull PsiFile psiFile, @NotNull String content) {

        // bundle scope
        if(content.startsWith("@")) {
            return Collections.emptyList();
        }

        PsiDirectory containingDirectory = psiFile.getContainingDirectory();
        if(containingDirectory == null) {
            return Collections.emptyList();
        }

        VirtualFile relativeFile = VfsUtil.findRelativeFile(content, containingDirectory.getVirtualFile());
        if(relativeFile == null) {
            return Collections.emptyList();
        }

        Set<PsiFile> psiFiles = new HashSet<>();
        if (relativeFile.isDirectory()) {
            String path = relativeFile.getPath();
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:/**/*.php");

            Set<String> files = new HashSet<>();
            try {
                Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                        if (pathMatcher.matches(path)) {
                            files.add(path.toString());
                        }
                        return files.size() < 200 ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {
            }

            Set<PsiFile> collect = files.stream()
                .map(s -> VfsUtil.findFileByIoFile(new File(s), false))
                .filter(Objects::nonNull)
                .map(virtualFile -> PsiElementUtils.virtualFileToPsiFile(psiFile.getProject(), virtualFile))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            psiFiles.addAll(collect);
        } else {
            PsiFile targetFile = PsiElementUtils.virtualFileToPsiFile(psiFile.getProject(), relativeFile);
            if(targetFile != null) {
                psiFiles.add(targetFile);
            }
        }

        return psiFiles;
    }

    private record FileResourceNotNullLazyValue(Project project, VirtualFile virtualFile) implements Supplier<Collection<? extends PsiElement>> {
        private FileResourceNotNullLazyValue(@NotNull Project project, @NotNull VirtualFile virtualFile) {
            this.project = project;
            this.virtualFile = virtualFile;
        }

        @Override
        public Collection<? extends PsiElement> get() {
            Collection<PsiElement> psiElements = new HashSet<>();

            for (Pair<VirtualFile, String> pair : getFileResources(project, virtualFile)) {
                PsiFile psiFile1 = PsiElementUtils.virtualFileToPsiFile(project, pair.getFirst());
                if (psiFile1 == null) {
                    continue;
                }

                FileResourceVisitorUtil.visitFile(psiFile1, fileResourceConsumer -> {
                    if (fileResourceConsumer.getResource().equalsIgnoreCase(pair.getSecond())) {
                        psiElements.add(fileResourceConsumer.getPsiElement());
                    }
                });
            }

            return psiElements;
        }
    }

    /**
     * Split given "resource" path to get the root path and its pattern
     *
     * @param resourcePath "../src/{Entity,Foobar}/"
     * @return "../src", "{Entity,Foobar}"
     */
    @NotNull
    public static Pair<String, String> getGlobalPatternDirectory(@NotNull String resourcePath) {
        String[] split = resourcePath.split("/");
        List<String> path = new ArrayList<>();

        for (int i = 0; i < split.length; i++) {
            String s1 = split[i];
            if (Stream.of("$", "*", "[", "]", "|", "(", ")", "?", "{", "}").anyMatch(s1::contains)) {
                String join = String.join("/", Arrays.copyOfRange(split, i, split.length));

                if (resourcePath.endsWith("/") && !join.endsWith("/")) {
                    join += "/";
                }

                return new Pair<>(String.join("/", path), join);
            }

            path.add(s1);
        }

        String join = String.join("/", path);
        if (resourcePath.endsWith("/") && !join.endsWith("/")) {
            join += "/";
        }

        return new Pair<>(join, null);
    }

    public static @NotNull NavigationGutterIconBuilder<PsiElement> getNavigationGutterForRouteAnnotationResources(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull String resource) {
        return NavigationGutterIconBuilder.create(AllIcons.Modules.SourceRoot)
            .setTargets(NotNullLazyValue.lazy(() -> {
                String r = resource;

                // try to resolve the following pattern; all are recursive search for php
                // ../../Core/**/**/Controller/*Controller.php
                // ../src/Controller/
                // ../src/Kernel.php
                if (resource.endsWith("/") || resource.endsWith("\\")) {
                    r += "**.php";
                } else if (Arrays.stream(FileResourceUtil.GLOB_DETECTION_CHARS).noneMatch(r::contains) && !resource.toLowerCase().endsWith(".php")) {
                    r += "**.php";
                }

                Collection<VirtualFile> filesForResources = FileResourceUtil.getFilesForResources(project, virtualFile, r)
                    .stream()
                    .filter(virtualFile1 -> "php".equalsIgnoreCase(virtualFile1.getExtension())).collect(Collectors.toSet());

                return PsiElementUtils.convertVirtualFilesToPsiFiles(project, filesForResources);
            }))
            .setTooltipText("Navigate to matching files");
    }

    /**
     * Find a file based on "glob" and its content path.
     *
     * @param virtualFile Current scope, parent (directory) is taking for resolving root dirctory to glob operation
     * @param glob "@FooBundle/foo.xml", "src/foo.xml", "src/.../src.xml"
     */
    @NotNull
    public static Collection<VirtualFile> getFilesForResources(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull String glob) {
        VirtualFile parent = virtualFile.getParent();
        if (parent == null) {
            return Collections.emptyList();
        }

        if (glob.startsWith("@")) {
            String replace = glob.replace("\\", "/");
            int i = replace.indexOf("/");

            boolean resolved = false;

            if (i > 2 || i == -1) {
                if (i == -1) {
                    i = glob.length();
                }

                String substring = glob.substring(1, i);


                for (SymfonyBundle symfonyBundle : new SymfonyBundleUtil(project).getBundle(substring)) {
                    PsiDirectory directory1 = symfonyBundle.getDirectory();
                    if (directory1 == null) {
                        continue;
                    }

                    glob = glob.substring(replace.contains("/") ? i + 1 : replace.length());
                    parent = directory1.getVirtualFile();

                    resolved = true;

                    break;
                }

            }

            if (!resolved) {
                return Collections.emptyList();
            }

        }

        Path normalize = Paths.get(parent.getPath() + File.separatorChar + StringUtils.stripStart(glob, "\\/")).normalize();
        Pair<String, String> globalPatternDirectory = getGlobalPatternDirectory(normalize.toString());

        Collection<VirtualFile> files = new HashSet<>();
        if (globalPatternDirectory.getSecond() == null) {
            VirtualFile target = VfsUtil.findFile(Paths.get(globalPatternDirectory.getFirst()), false);
            if (target != null) {
                files.add(target);
            }

            return files;
        }

        try {
            for (Path file : Files.newDirectoryStream(Paths.get(globalPatternDirectory.getFirst()), globalPatternDirectory.getSecond())) {
                VirtualFile target = VfsUtil.findFile(file, false);
                if (target != null) {
                    files.add(target);
                }
            }
        } catch (PatternSyntaxException | IOException ignored) {
        }

        return files;
    }
}
