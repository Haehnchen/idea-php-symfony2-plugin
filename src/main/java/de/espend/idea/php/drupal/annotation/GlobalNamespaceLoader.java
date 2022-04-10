package de.espend.idea.php.drupal.annotation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.annotation.extension.PhpAnnotationGlobalNamespacesLoader;
import de.espend.idea.php.annotation.extension.parameter.AnnotationGlobalNamespacesLoaderParameter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GlobalNamespaceLoader implements PhpAnnotationGlobalNamespacesLoader {

    private static final Key<CachedValue<Collection<String>>> CACHE = new Key<>("DRUPAL_GLOBAL_NAMESPACE");

    @Override
    @NotNull
    public Collection<String> getGlobalNamespaces(@NotNull AnnotationGlobalNamespacesLoaderParameter parameter) {
        Project project = parameter.getProject();

        CachedValue<Collection<String>> cache = project.getUserData(CACHE);

        if(cache == null) {
            cache = CachedValuesManager.getManager(project).createCachedValue(() ->
                CachedValueProvider.Result.create(getGlobalNamespacesInner(project), PsiModificationTracker.MODIFICATION_COUNT), false
            );

            project.putUserData(CACHE, cache);
        }

        return cache.getValue();
    }

    @NotNull
    private static Collection<String> getGlobalNamespacesInner(@NotNull Project project) {
        Collection<String> namespaces = new HashSet<>();

        for (PhpClass phpClass : PhpIndex.getInstance(project).getAllSubclasses("Drupal\\Component\\Annotation\\AnnotationInterface")) {
            String namespaceName = StringUtils.strip(phpClass.getNamespaceName(), "\\");
            if(namespaceName.endsWith("Annotation")) {
                namespaces.add(namespaceName);
            }
        }

        return namespaces;
    }
}
