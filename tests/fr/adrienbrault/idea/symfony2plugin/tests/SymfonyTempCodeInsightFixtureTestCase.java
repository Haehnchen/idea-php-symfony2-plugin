package fr.adrienbrault.idea.symfony2plugin.tests;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class SymfonyTempCodeInsightFixtureTestCase extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // clear super fixtures instances
        this.myFixture.tearDown();

        // heavy project
        // @TODO: still no index process
        IdeaProjectTestFixture fixtures = IdeaTestFixtureFactory.getFixtureFactory()
            .createFixtureBuilder(RandomStringUtils.randomAlphanumeric(20))
            .getFixture();

        this.myFixture = JavaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(fixtures, new LightTempDirTestFixtureImpl(true));

        this.myFixture.setUp();
        this.myModule = this.myFixture.getModule();

        Settings.getInstance(getProject()).pluginEnabled = true;
    }

    @NotNull
    protected VirtualFile createFile(@NotNull String file) {
        return createFile(file, null);
    }

    @NotNull
    protected VirtualFile[] createFiles(@NotNull String... files) {
        List<VirtualFile> virtualFiles = new ArrayList<>();

        for (String file : files) {
            virtualFiles.add(createFile(file));
        }

        return virtualFiles.toArray(new VirtualFile[virtualFiles.size()]);
    }

    protected VirtualFile createFile(@NotNull String file, @Nullable String content) {
        return ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
            @Override
            public VirtualFile compute() {
                VirtualFile virtualFile = null;
                try {
                    String[] paths = file.split("/");

                    if (paths.length == 0) {
                        virtualFile = getProject().getBaseDir().createChildData(this, file);
                    } else {
                        virtualFile = VfsUtil.createDirectoryIfMissing(
                            getProject().getBaseDir(),
                            StringUtils.join(Arrays.copyOf(paths, paths.length - 1), "/")
                        ).createChildData(this, paths[paths.length - 1]);
                    }

                    if (content != null) {
                        virtualFile.setBinaryContent(content.getBytes());
                    }
                } catch (IOException ignored) {
                }

                return virtualFile;
            }
        });
    }
}
