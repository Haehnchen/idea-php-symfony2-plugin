package fr.adrienbrault.idea.symfony2plugin.asset.dic;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;

import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;

import java.util.ArrayList;
import java.util.List;

public class AssetDirectoryReader {

    protected Project project;
    protected boolean includeBundleDir = false;
    protected String filterExtension;

    public void setProject(Project project) {
        this.project = project;
    }

    public AssetDirectoryReader setIncludeBundleDir(boolean includeBundleDir) {
        this.includeBundleDir = includeBundleDir;
        return this;
    }

    public AssetDirectoryReader setFilterExtension(String filterExtension) {
        this.filterExtension = filterExtension;
        return this;
    }

    public List<AssetFile> getAssetFiles() {
        final List<AssetFile> files = new ArrayList<AssetFile>();

        VirtualFile projectDirectory = project.getBaseDir();
        final VirtualFile webDirectory = VfsUtil.findRelativeFile(projectDirectory, "Web");

        if (null == webDirectory) {
            return files;
        }

        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        fileIndex.iterateContentUnderDirectory(webDirectory, new ContentIterator() {
            @Override
            public boolean processFile(final VirtualFile virtualFile) {
                if(isValidFile(virtualFile)) {
                    files.add(new AssetFile(virtualFile, AssetEnum.Position.Web, webDirectory));
                }
                return true;
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
            VirtualFile blaDirectory = VfsUtil.findRelativeFile(bundleDirectoryVirtual, "Resources", "public");

            if (null != blaDirectory) {

                fileIndex.iterateContentUnderDirectory(blaDirectory, new ContentIterator() {
                    @Override
                    public boolean processFile(final VirtualFile virtualFile) {

                        if(isValidFile(virtualFile)) {
                            files.add(new AssetFile(virtualFile, AssetEnum.Position.Bundle, bundleDirectoryVirtual, '@' + bundle.getName() + "/"));
                        }

                        return true;
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
            if(null != extension) {
               return extension.equals(this.filterExtension);
            }
        }

        return true;
    }

}
