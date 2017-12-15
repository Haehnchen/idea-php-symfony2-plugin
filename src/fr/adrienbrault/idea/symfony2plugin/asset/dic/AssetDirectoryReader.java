package fr.adrienbrault.idea.symfony2plugin.asset.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetDirectoryReader {
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
    public static VirtualFile getProjectAssetRoot(@NotNull Project project) {
        VirtualFile projectDirectory = project.getBaseDir();
        String webDirectoryName = Settings.getInstance(project).directoryToWeb;
        return VfsUtil.findRelativeFile(projectDirectory, webDirectoryName.split("/"));
    }

    public List<AssetFile> getAssetFiles(@NotNull Project project) {
        List<AssetFile> files = new ArrayList<>();

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

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(PhpIndex.getInstance(project));
        for(final SymfonyBundle bundle : symfonyBundleUtil.getBundles()) {
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

    private boolean isValidFile(@NotNull VirtualFile virtualFile) {
        if (this.filterExtension.size() == 0 || virtualFile.isDirectory()) {
            return false;
        }

        String extension = virtualFile.getExtension();
        return extension != null && this.filterExtension.contains(extension);
    }
}
