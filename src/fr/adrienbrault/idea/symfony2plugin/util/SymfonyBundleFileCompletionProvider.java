package fr.adrienbrault.idea.symfony2plugin.util;


import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.dict.BundleFile;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ResourceFileInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundleFileLookupElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class SymfonyBundleFileCompletionProvider extends CompletionProvider<CompletionParameters> {

    private String path;

    public SymfonyBundleFileCompletionProvider(String path) {
        this.path = path;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

        if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
            return;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(completionParameters.getPosition().getProject());

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(phpIndex);
        ArrayList<BundleFile> bundleFiles = new ArrayList<BundleFile>();

        for(SymfonyBundle symfonyBundle : symfonyBundleUtil.getBundles()) {

            VirtualFile virtualFile = symfonyBundle.getRelative(this.path);
            if(virtualFile != null) {
                final BundleContentIterator bundleContentIterator = new BundleContentIterator(symfonyBundle, bundleFiles, completionParameters.getPosition().getProject());
                VfsUtil.visitChildrenRecursively(virtualFile, new VirtualFileVisitor() {
                    @Override
                    public boolean visitFile(@NotNull VirtualFile virtualFile) {
                        bundleContentIterator.processFile(virtualFile);
                        return super.visitFile(virtualFile);
                    }
                });
            }

        }

        for(BundleFile bundleFile : bundleFiles) {
            completionResultSet.addElement(new SymfonyBundleFileLookupElement(bundleFile, ResourceFileInsertHandler.getInstance()));
        }

    }

    private class BundleContentIterator implements ContentIterator{

        private SymfonyBundle symfonyBundle;
        private ArrayList<BundleFile> bundleFiles;
        private Project project;

        public BundleContentIterator(SymfonyBundle symfonyBundle, ArrayList<BundleFile> bundleFiles, Project project) {
            this.symfonyBundle = symfonyBundle;
            this.bundleFiles = bundleFiles;
            this.project = project;
        }

        @Override
        public boolean processFile(VirtualFile virtualFile) {
            if(!virtualFile.isDirectory()) {
                bundleFiles.add(new BundleFile(this.symfonyBundle, virtualFile, project));
            }

            return true;
        }
    }
}
