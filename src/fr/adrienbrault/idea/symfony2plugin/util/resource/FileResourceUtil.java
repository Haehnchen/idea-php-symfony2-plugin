package fr.adrienbrault.idea.symfony2plugin.util.resource;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIcons;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FileResourcesIndex;
import fr.adrienbrault.idea.symfony2plugin.util.FileResourceVisitorUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResourceUtil {

    /**
     * Search for files refers to given file
     */
    @NotNull
    public static Collection<VirtualFile> getFileResourceRefers(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        String bundleLocateName = getBundleLocateName(project, virtualFile);
        if(bundleLocateName == null) {
            return Collections.emptyList();
        }

        return getFileResourceRefers(project, bundleLocateName);
    }

    /**
     * Search for files refers to given file
     */
    @NotNull
    public static Collection<VirtualFile> getFileResourceRefers(@NotNull Project project, @NotNull String bundleLocateName) {
        return FileBasedIndex.getInstance().getContainingFiles(
            FileResourcesIndex.KEY,
            bundleLocateName,
            GlobalSearchScope.allScope(project)
        );
    }

    @Nullable
    public static String getBundleLocateName(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        SymfonyBundle containingBundle = new SymfonyBundleUtil(project).getContainingBundle(virtualFile);
        if(containingBundle == null) {
            return null;
        }

        String relativePath = containingBundle.getRelativePath(virtualFile);
        if(relativePath == null) {
            return null;
        }

        return "@" + containingBundle.getName() + "/" + relativePath;
    }

    /**
     * Search for line definition of "@FooBundle/foo.xml"
     */
    @NotNull
    private static Collection<PsiElement> getBundleLocateStringDefinitions(@NotNull Project project, final @NotNull String bundleFileName) {
        final Collection<PsiElement> psiElements = new HashSet<PsiElement>();
        for (VirtualFile refVirtualFile : getFileResourceRefers(project, bundleFileName)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(refVirtualFile);
            if(psiFile == null) {
                continue;
            }

            FileResourceVisitorUtil.visitFile(psiFile, new Consumer<FileResourceVisitorUtil.FileResourceConsumer>() {
                @Override
                public void consume(FileResourceVisitorUtil.FileResourceConsumer consumer) {
                    if (bundleFileName.equals(consumer.getResource())) {
                        psiElements.add(consumer.getPsiElement());
                    }
                }
            });
        }

        return psiElements;
    }

    @Nullable
    public static RelatedItemLineMarkerInfo<PsiElement> getFileImplementsLineMarker(@NotNull PsiFile psiFile) {
        final Project project = psiFile.getProject();
        String bundleLocateName = FileResourceUtil.getBundleLocateName(project, psiFile.getVirtualFile());
        if(bundleLocateName == null) {
            return null;
        }

        if(FileResourceUtil.getFileResourceRefers(project, bundleLocateName).size() == 0) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTS).
            setTargets(new FileResourceUtil.FileResourceNotNullLazyValue(project, bundleLocateName)).
            setTooltipText("Navigate to resource");

        return builder.createLineMarkerInfo(psiFile);
    }

    /**
     * On route annotations we can have folder scope so: "@FooBundle/Controller/foo.php" can be equal "@FooBundle/Controller/"
     */
    @Nullable
    public static RelatedItemLineMarkerInfo<PsiElement> getFileImplementsLineMarkerInFolderScope(@NotNull PsiFile psiFile) {
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if(virtualFile == null) {
            return null;
        }

        final Project project = psiFile.getProject();
        String bundleLocateName = FileResourceUtil.getBundleLocateName(project, virtualFile);
        if(bundleLocateName == null) {
            return null;
        }

        Set<String> names = new HashSet<String>();
        names.add(bundleLocateName);

        // strip filename
        int i = bundleLocateName.lastIndexOf("/");
        if(i > 0) {
            names.add(bundleLocateName.substring(0, i));
        }

        int targets = 0;
        for (String name : names) {
            targets += FileResourceUtil.getFileResourceRefers(project, name).size();
        }

        if(targets == 0) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTS).
            setTargets(new FileResourceUtil.FileResourceNotNullLazyValue(project, names)).
            setTooltipText("Navigate to resource");

        return builder.createLineMarkerInfo(psiFile);
    }

    private static class FileResourceNotNullLazyValue extends NotNullLazyValue<Collection<? extends PsiElement>> {

        private final Collection<String> resources;
        private final Project project;

        public FileResourceNotNullLazyValue(@NotNull Project project, @NotNull Collection<String> resource) {
            this.resources = resource;
            this.project = project;
        }

        public FileResourceNotNullLazyValue(@NotNull Project project, @NotNull String resource) {
            this.resources = Collections.singleton(resource);
            this.project = project;
        }

        @NotNull
        @Override
        protected Collection<? extends PsiElement> compute() {
            Collection<PsiElement> psiElements = new HashSet<PsiElement>();

            for (String resource : this.resources) {
                psiElements.addAll(getBundleLocateStringDefinitions(project, resource));
            }

            return psiElements;
        }
    }
}
