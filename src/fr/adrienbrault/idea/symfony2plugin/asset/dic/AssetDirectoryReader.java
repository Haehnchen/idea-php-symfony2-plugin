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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetDirectoryReader {

    protected Project project;
    protected boolean includeBundleDir = false;
    protected String[] filterExtension;

    public AssetDirectoryReader setProject(Project project) {
        this.project = project;
        return this;
    }

    public AssetDirectoryReader setIncludeBundleDir(boolean includeBundleDir) {
        this.includeBundleDir = includeBundleDir;
        return this;
    }

    public AssetDirectoryReader setFilterExtension(String... filterExtension) {
        this.filterExtension = filterExtension;
        return this;
    }

    @Nullable
    public static VirtualFile getProjectAssetRoot(@NotNull Project project) {
        VirtualFile projectDirectory = project.getBaseDir();
        String webDirectoryName = Settings.getInstance(project).directoryToWeb;
        return VfsUtil.findRelativeFile(projectDirectory, webDirectoryName.split("/"));
    }

    public List<AssetFile> getAssetFiles() {
        final List<AssetFile> files = new ArrayList<>();

        final VirtualFile webDirectory = getProjectAssetRoot(project);
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

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(PhpIndex.getInstance(this.project));
        for(final SymfonyBundle bundle : symfonyBundleUtil.getBundles()) {

            PsiDirectory bundleDirectory = bundle.getDirectory();
            if(null == bundleDirectory) {
                continue;
            }

            final VirtualFile bundleDirectoryVirtual = bundleDirectory.getVirtualFile();
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

    private boolean isValidFile(VirtualFile virtualFile) {

        if (virtualFile.isDirectory()) {
            return false;
        }

        if (this.filterExtension != null) {
            String extension = virtualFile.getExtension();

            // file need extension and it must be in list
            return null != extension && Arrays.asList(this.filterExtension).contains(extension);

        }

        return true;
    }

}
