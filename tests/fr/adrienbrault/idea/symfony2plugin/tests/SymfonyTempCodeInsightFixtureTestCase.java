package fr.adrienbrault.idea.symfony2plugin.tests;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import org.apache.commons.lang.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class SymfonyTempCodeInsightFixtureTestCase extends UsefulTestCase {
    private Project project;

    private IdeaProjectTestFixture myFixture;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        myFixture = IdeaTestFixtureFactory.getFixtureFactory()
            .createFixtureBuilder(RandomStringUtils.randomAlphanumeric(20))
            .getFixture();

        myFixture.setUp();

        project = myFixture.getProject();
        Settings.getInstance(project).pluginEnabled = true;
    }

    protected void tearDown() throws Exception {
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
    protected VirtualFile createFile(@NotNull String path, @NotNull String file) {
        final VirtualFile[] childData = new VirtualFile[1];

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    childData[0] = VfsUtil.createDirectoryIfMissing(getProject().getBaseDir(), path).createChildData(this, file);
                } catch (IOException ignored) {
                }
            }
        });

        return childData[0];
    }

    protected VirtualFile createFile(@NotNull String path, @NotNull String file, @NotNull String content) {
        final VirtualFile[] childData = new VirtualFile[1];

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    childData[0] = VfsUtil.createDirectoryIfMissing(getProject().getBaseDir(), path).createChildData(this, file);
                    childData[0].setBinaryContent(content.getBytes());
                } catch (IOException ignored) {
                }
            }
        });

        return childData[0];
    }
}
