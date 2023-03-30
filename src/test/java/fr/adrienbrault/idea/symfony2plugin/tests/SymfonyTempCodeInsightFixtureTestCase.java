package fr.adrienbrault.idea.symfony2plugin.tests;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.*;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Exactly the same as LightCodeInsightFixtureTestCase except uses TempDirTestFixtureImpl instead of LightTempDirTestFixtureImpl.
 * This is because the light temp dir stuff fails to work in some cases because it's in-memory file system protocol "temp:" is
 * invalid in the eyes of the URL class, which causes URL exceptions.  For instance, the Json manifold creates a URL from the resource files.
 *
 * see https://www.programcreek.com/java-api-examples/?code=manifold-systems/manifold-ij/manifold-ij-master/src/test/java/manifold/ij/SomewhatLightCodeInsightFixtureTestCase.java
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class SymfonyTempCodeInsightFixtureTestCase extends UsefulTestCase {
    private Project project;

    private IdeaProjectTestFixture myFixture;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = IdeaTestFixtureFactory.getFixtureFactory()
            .createLightFixtureBuilder(new DefaultLightProjectDescriptor(), "MyProject");

        myFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
            fixtureBuilder.getFixture(),
            IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture()
        );

        myFixture.setUp();

        project = myFixture.getProject();
        Settings.getInstance(project).pluginEnabled = true;
    }

    protected void tearDown() throws Exception {
        this.project = null;

        try {
            this.myFixture.tearDown();
        } finally {
            this.myFixture = null;
            super.tearDown();
        }
    }

    @NotNull
    protected Project getProject() {
        return project;
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

        return virtualFiles.toArray(new VirtualFile[0]);
    }

    protected VirtualFile createFile(@NotNull String file, @Nullable String content) {
        final VirtualFile[] childData = new VirtualFile[1];

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] paths = file.split("/");

                    if(paths.length == 0) {
                        childData[0] = getProject().getBaseDir().createChildData(this, file);
                    } else {
                        childData[0] = VfsUtil.createDirectoryIfMissing(
                            getProject().getBaseDir(),
                            StringUtils.join(Arrays.copyOf(paths, paths.length - 1), "/")
                        ).createChildData(this, paths[paths.length - 1]);
                    }

                    if(content != null) {
                        childData[0].setBinaryContent(content.getBytes());
                    }
                } catch (IOException ignored) {
                }
            }
        });

        return childData[0];
    }
}
