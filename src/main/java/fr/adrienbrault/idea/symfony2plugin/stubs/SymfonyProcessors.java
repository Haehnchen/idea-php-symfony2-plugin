package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyProcessors {

    @Deprecated
    public static class CollectProjectUniqueKeys implements Processor<String> {
        @NotNull
        final Project project;

        @NotNull
        final ID<String, ?>  id;

        @NotNull
        final Set<String> stringSet;

        @Deprecated
        public CollectProjectUniqueKeys(@NotNull Project project, @NotNull ID<String, ?>  id) {
            this.project = project;
            this.id = id;
            this.stringSet = new HashSet<>();
        }

        @Override
        public boolean process(String s) {
            this.stringSet.add(s);
            return true;
        }

        @NotNull
        public Set<String> getResult() {
            return stringSet.stream()
                .filter(
                    s -> !FileBasedIndex.getInstance().getContainingFiles(id, s, GlobalSearchScope.allScope(project)).isEmpty())
                .collect(Collectors.toSet()
            );
        }
    }

    @Deprecated
    public static class CollectProjectUniqueKeysStrong implements Processor<String> {
        @NotNull
        final Project project;

        @NotNull
        final ID<String, ?>  id;

        @NotNull
        final Set<String> stringSet;

        @NotNull
        final Collection<String> strongKeys;

        @Deprecated
        public CollectProjectUniqueKeysStrong(@NotNull Project project, @NotNull ID<String, ?>  id, @NotNull Collection<String> strongKeys) {
            this.project = project;
            this.id = id;
            this.strongKeys = strongKeys;
            this.stringSet = new HashSet<>();
        }

        @Override
        public boolean process(String s) {
            if(!strongKeys.contains(s)) {
                this.stringSet.add(s);
            }
            return true;
        }

        @NotNull
        public Set<String> getResult() {
            return stringSet.stream()
                .filter(
                    s -> !FileBasedIndex.getInstance().getContainingFiles(id, s, GlobalSearchScope.allScope(project)).isEmpty()
                )
                .collect(Collectors.toSet()
            );
        }
    }

    @NotNull
    public static Set<String> createResult(@NotNull Project project, @NotNull ID<String, ?> id) {
        return new HashSet<>(IndexUtil.getAllKeysForProject(id, project));
    }

    @Deprecated
    @NotNull
    public static Set<String> createResult(@NotNull Project project, @NotNull ID<String, ?>  id, @NotNull Collection<String> strongKeys) {
        CollectProjectUniqueKeysStrong collector = new CollectProjectUniqueKeysStrong(project, id, strongKeys);
        FileBasedIndex.getInstance().processAllKeys(id, collector, project);
        return collector.getResult();
    }
}
