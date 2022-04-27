package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.util.SimpleModificationTracker;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AbsoluteFileModificationTracker extends SimpleModificationTracker {
    @NotNull
    private final Collection<String> files;
    private long last = 0;

    public AbsoluteFileModificationTracker(@NotNull Collection<String> files) {
        this.files = files;
    }
    @Override
    public long getModificationCount() {
        long hash = this.files.stream()
            .sorted()
            .map(File::new)
            .mapToLong(value -> value.exists() ? value.lastModified() : 0)
            .sum();

        if (hash != this.last) {
            this.last = hash;
            this.incModificationCount();
        }

        return super.getModificationCount();
    }
}
