package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
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
        // Uses VirtualFile.getTimeStamp() - cached in VFS, no filesystem I/O
        long hash = this.files.stream()
            .sorted()
            .mapToLong(path -> {
                VirtualFile vf = VfsUtil.findFileByIoFile(new File(path), false);
                return vf != null && vf.exists() ? vf.getTimeStamp() : 0;
            })
            .sum();

        if (hash != this.last) {
            this.last = hash;
            this.incModificationCount();
        }

        return super.getModificationCount();
    }
}
