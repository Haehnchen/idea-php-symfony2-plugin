package fr.adrienbrault.idea.symfony2plugin.config.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.FilesystemUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigUtil {

    private static final Key<CachedValue<Map<String, Collection<String>>>> TREE_SIGNATURE_CACHE = new Key<>("TREE_SIGNATURE_CACHE");

    /**
     * Search for all "Symfony\Component\Config\Definition\Builder\TreeBuilder::root" elements
     */
    @NotNull
    public static Map<String, Collection<String>> getTreeSignatures(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TREE_SIGNATURE_CACHE,
            () -> CachedValueProvider.Result.create(visitTreeSignatures(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    public static Collection<PsiElement> getTreeSignatureTargets(@NotNull Project project, @NotNull String key) {
        Map<String, Collection<String>> signatures = getTreeSignatures(project);
        if(!signatures.containsKey(key)) {
            return Collections.emptyList();
        }

        Collection<PhpClass> phpClasses = new HashSet<>();
        for (String aClass : signatures.get(key)) {
            ContainerUtil.addIfNotNull(phpClasses, PhpElementsUtil.getClass(project, aClass));
        }

        Collection<PsiElement> psiElements = new ArrayList<>();
        visitTreeSignatures(phpClasses, treeVisitor -> {
            if(key.equalsIgnoreCase(treeVisitor.contents)) {
                psiElements.add(treeVisitor.psiElement);
            }
        });

        return psiElements;
    }

    @NotNull
    public static Collection<PsiElement> getTreeSignatureTargets(@NotNull Project project, @NotNull String key, @NotNull Collection<String> classes) {
        Collection<PsiElement> psiElements = new ArrayList<>();

        Collection<PhpClass> phpClasses = new HashSet<>();
        for (String aClass : classes) {
            ContainerUtil.addIfNotNull(phpClasses, PhpElementsUtil.getClass(project, aClass));
        }

        visitTreeSignatures(phpClasses, treeVisitor -> {
            if(key.equalsIgnoreCase(treeVisitor.contents)) {
                psiElements.add(treeVisitor.psiElement);
            }
        });

        return psiElements;
    }

    @NotNull
    private static Map<String, Collection<String>> visitTreeSignatures(@NotNull Project project) {
        Map<String, Collection<String>> signatures = new HashMap<>();

        visitTreeSignatures(PhpIndex.getInstance(project).getAllSubclasses("Symfony\\Component\\Config\\Definition\\ConfigurationInterface"), treeVisitor -> {
            if(!signatures.containsKey(treeVisitor.contents)) {
                signatures.put(treeVisitor.contents, new HashSet<>());
            }

            signatures.get(treeVisitor.contents).add(treeVisitor.phpClass.getFQN());
        });

        return signatures;
    }

    private static void visitTreeSignatures(@NotNull Collection<PhpClass> classes, @NotNull Consumer<TreeVisitor> consumer) {
        for (PhpClass phpClass : classes) {
            Method method = phpClass.findOwnMethodByName("getConfigTreeBuilder");
            if(method == null) {
                continue;
            }

            Collection<ParameterListOwner> parameterOwners = new ArrayList<>();

            // Symfony < 4.1: (new TreeBuilder())->root('foobar')
            for(MethodReference methodReference: PsiTreeUtil.findChildrenOfType(method, MethodReference.class)) {
                if(!PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Symfony\\Component\\Config\\Definition\\Builder\\TreeBuilder", "root")) {
                    continue;
                }

                parameterOwners.add(methodReference);
            }

            // Symfony >= 4.1: new TreeBuilder('foobar')
            for(NewExpression methodReference: PsiTreeUtil.findChildrenOfType(method, NewExpression.class)) {
                if(!PhpElementsUtil.isNewExpressionPhpClassWithInstance(methodReference, "Symfony\\Component\\Config\\Definition\\Builder\\TreeBuilder")) {
                    continue;
                }

                parameterOwners.add(methodReference);
            }

            for (ParameterListOwner ownerParameters : parameterOwners) {
                PsiElement[] parameters = ownerParameters.getParameters();
                if (parameters.length == 0) {
                    continue;
                }

                String contents = PhpElementsUtil.getStringValue(parameters[0]);
                if(contents == null || StringUtils.isBlank(contents)) {
                    continue;
                }

                consumer.accept(new TreeVisitor(phpClass, parameters[0], contents));
            }
        }
    }

    private static class TreeVisitor {
        @NotNull
        private final PhpClass phpClass;

        @NotNull
        private final PsiElement psiElement;

        @NotNull
        private final String contents;

        public TreeVisitor(@NotNull PhpClass phpClass, @NotNull PsiElement psiElement, @NotNull String contents) {

            this.phpClass = phpClass;
            this.psiElement = psiElement;
            this.contents = contents;
        }
    }

    /**
     * app/config[..].yml
     * app/config[..].yaml
     * config/packages/twig.yml
     * config/packages/twig.yaml
     */
    public static Collection<VirtualFile> getConfigurations(@NotNull Project project, @NotNull String packageName) {
        Collection<String[]> paths = Arrays.asList(
            new String[] {"config", "packages", packageName +".yml"},
            new String[] {"config", "packages", packageName +".yaml"},
            new String[] {"config", "packages", packageName, "config.yaml"},
            new String[] {"config", "packages", packageName, "config.yml"}
        );

        Collection<VirtualFile> virtualFiles = new HashSet<>();

        for (String[] path : paths) {
            VirtualFile configFile = VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), path);
            if(configFile != null) {
                virtualFiles.add(configFile);
            }
        }

        // note
        for (VirtualFile virtualFile : FilesystemUtil.getAppDirectories(project)) {
            VirtualFile configDir = VfsUtil.findRelativeFile(virtualFile, "config");
            if(configDir != null) {
                for (VirtualFile configFile : configDir.getChildren()) {
                    // app/config/config*yml
                    if(configFile.getFileType() == YAMLFileType.YML && configFile.getName().startsWith("config")) {
                        virtualFiles.add(configFile);
                    }
                }
            }
        }

        return virtualFiles;
    }
}
