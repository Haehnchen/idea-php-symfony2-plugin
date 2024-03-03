package fr.adrienbrault.idea.symfony2plugin.dic.webDeployment;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.webDeployment.dict.ServiceParameterStorage;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.storage.RemoteFileStorageInterface;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.apache.commons.vfs2.FileObject;
import org.apache.sanselan.util.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerRemoteFileStorage implements RemoteFileStorageInterface<ServiceParameterStorage> {

    private ServiceParameterStorage storage = new ServiceParameterStorage(
        Collections.emptyList()
    );

    @NotNull
    @Override
    public Collection<String> files(@NotNull Project project) {
        return RemoteWebServerUtil.getRemoteAbleFiles(Settings.getInstance(project).containerFiles);
    }

    @Override
    public void build(@NotNull Project project, @NotNull Collection<FileObject> fileObjects) {

        Collection<InputStream> memoryCache = new ArrayList<>();

        for (FileObject fileObject : fileObjects) {
            InputStream inputStream;
            try {
                // copy stream
                inputStream = new ResetOnCloseInputStream(new ByteArrayInputStream(
                    IOUtils.getInputStreamBytes(fileObject.getContent().getInputStream()))
                );
            } catch (IOException ignored) {
                continue;
            }

            memoryCache.add(inputStream);
        }

        storage = new ServiceParameterStorage(memoryCache);
    }

    @NotNull
    @Override
    public ServiceParameterStorage getState() {
        return storage;
    }

    @Override
    public void clear() {
        storage = new ServiceParameterStorage(
            Collections.emptyList()
        );
    }

    private static class ResetOnCloseInputStream extends InputStream {

        private final InputStream decorated;

        public ResetOnCloseInputStream(InputStream inputStream) {
            if (!inputStream.markSupported()) {
                throw new IllegalArgumentException("marking not supported");
            }
            inputStream.mark( 1 << 32);
            decorated = inputStream;
        }

        @Override
        public void close() throws IOException {
            decorated.reset();
        }

        @Override
        public int read() throws IOException {
            return decorated.read();
        }
    }
}


