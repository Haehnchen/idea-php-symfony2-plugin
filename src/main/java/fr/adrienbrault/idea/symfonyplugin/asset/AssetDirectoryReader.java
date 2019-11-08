package fr.adrienbrault.idea.symfonyplugin.asset;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiDirectory;
import fr.adrienbrault.idea.symfonyplugin.Settings;
import fr.adrienbrault.idea.symfonyplugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfonyplugin.util.dict.SymfonyBundle;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetDirectoryReader {

    public static AssetDirectoryReader INSTANCE = new  AssetDirectoryReader();

    final private boolean includeBundleDir;

    @NotNull
    final private Collection<String> filterExtension = new HashSet<>();

    public AssetDirectoryReader() {
        includeBundleDir = false;
    }

    public AssetDirectoryReader(@NotNull String[] filterExtension, boolean includeBundleDir) {
        this.includeBundleDir = includeBundleDir;
        this.filterExtension.addAll(Arrays.asList(filterExtension));
    }

    @Nullable
    private static VirtualFile getProjectAssetRoot(@NotNull Project project) {
        VirtualFile projectDirectory = project.getBaseDir();
        String webDirectoryName = Settings.getInstance(project).directoryToWeb;
        return VfsUtil.findRelativeFile(projectDirectory, webDirectoryName.split("/"));
    }

    @NotNull
    public Collection<AssetFile> getAssetFiles(@NotNull Project project) {
        Collection<AssetFile> files = new ArrayList<>();

        VirtualFile webDirectory = getProjectAssetRoot(project);
        if (null == webDirectory) {
            return files;
        }

        VfsUtil.visitChildrenRecursively(webDirectory, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile virtualFile) {
                if(isValidFile(virtualFile)) {
                    files.add(new AssetFile(virtualFile, AssetEnum.Position.Web, webDirectory));
                }
                return super.visitFile(virtualFile);
            }
        });

        if(!this.includeBundleDir) {
            return files;
        }

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(project);
        for(SymfonyBundle bundle : symfonyBundleUtil.getBundles()) {
            PsiDirectory bundleDirectory = bundle.getDirectory();
            if(null == bundleDirectory) {
                continue;
            }

            VirtualFile bundleDirectoryVirtual = bundleDirectory.getVirtualFile();
            VirtualFile resourceDirectory = VfsUtil.findRelativeFile(bundleDirectoryVirtual, "Resources");

            if (null != resourceDirectory) {
                VfsUtil.visitChildrenRecursively(resourceDirectory, new VirtualFileVisitor() {
                    @Override
                    public boolean visitFile(@NotNull VirtualFile virtualFile) {
                        if(isValidFile(virtualFile)) {
                            files.add(new AssetFile(virtualFile, AssetEnum.Position.Bundle, bundleDirectoryVirtual, '@' + bundle.getName() + "/"));
                        }
                        return super.visitFile(virtualFile);
                    }
                });
            }
        }

        return files;
    }

    /**
     * '@SampleBundle/Resources/public/js/*'
     * 'assets/js/*'
     * 'assets/js/*.js'
     */
    @NotNull
    public Collection<VirtualFile> resolveAssetFile(@NotNull Project project, @NotNull String filename) {
        String assetName = StringUtils.stripStart(filename.replace("\\", "/").replaceAll("/+", "/"), "/");

        // '@SampleBundle/Resources/public/js/foo,js'
        // TODO: '@SampleBundle/Resources/public/js/*'
        // TODO: '@SampleBundle/Resources/public/js/*.js'
        if(filename.startsWith("@")) {
            Collection<VirtualFile> files = new ArrayList<>();

            int i = filename.indexOf("/");
            if(i > 0) {
                String relativeFilename = filename.substring(1, i);
                for (SymfonyBundle bundle : new SymfonyBundleUtil(project).getBundle(relativeFilename)) {
                    String assetPath = filename.substring(i + 1);

                    Matcher matcher = Pattern.compile("^(.*[/\\\\])\\*([.\\w+]*)$").matcher(assetPath);
                    if (!matcher.find()) {
                        VirtualFile relative = bundle.getRelative(assetPath);
                        if(relative != null) {
                            files.add(relative);
                        }
                    } else {
                        // "/*"
                        // "/*.js"
                        PsiDirectory directory = bundle.getDirectory();
                        if(directory != null) {
                            files.addAll(collectWildcardDirectories(matcher, directory.getVirtualFile()));
                        }
                    }
                }
            }

            return files;
        }

        Collection<VirtualFile> files = new ArrayList<>();

        VirtualFile webDirectory = getProjectAssetRoot(project);
        if (null == webDirectory) {
            return files;
        }

        Matcher matcher = Pattern.compile("^(.*[/\\\\])\\*([.\\w+]*)$").matcher(assetName);
        if (!matcher.find()) {
            VirtualFile assetFile = VfsUtil.findRelativeFile(webDirectory, assetName.split("/"));
            if(assetFile != null) {
                files.add(assetFile);
            }
        } else {
            // "/*"
            // "/*.js"
            files.addAll(collectWildcardDirectories(matcher, webDirectory));
        }

        return files;
    }

    private Collection<VirtualFile> collectWildcardDirectories(@NotNull Matcher matcher, @NotNull VirtualFile directory) {
        Collection<VirtualFile> files = new HashSet<>();

        String pathName = matcher.group(1);
        String fileExtension = matcher.group(2).length() > 0 ? matcher.group(2) : null;

        pathName = StringUtils.stripEnd(pathName, "/");

        if(fileExtension == null) {
            // @TODO: filter files
            // 'assets/js/*'
            VirtualFile assetFile = VfsUtil.findRelativeFile(directory, pathName.split("/"));
            if(assetFile != null) {
                files.add(assetFile);
            }
        } else {
            // @TODO: filter files
            // 'assets/js/*.js'
            VirtualFile assetFile = VfsUtil.findRelativeFile(directory, pathName.split("/"));
            if(assetFile != null) {
                files.add(assetFile);
            }
        }

        return files;
    }

    private boolean isValidFile(@NotNull VirtualFile virtualFile) {
        if (virtualFile.isDirectory()) {
            return false;
        }

        if(filterExtension.size() == 0) {
            return true;
        }

        String extension = virtualFile.getExtension();
        return extension != null && this.filterExtension.contains(extension);
    }
}
