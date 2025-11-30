package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlSchemaEditorNotificationProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlSchemaEditorNotificationProvider
 */
public class YamlSchemaEditorNotificationProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        // Create a mock schema file in vendor directory
        myFixture.addFileToProject(
            "vendor/symfony/dependency-injection/Loader/schema/services.schema.json",
            "{\"$schema\": \"http://json-schema.org/draft-07/schema#\"}"
        );
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/yaml/fixtures";
    }

    /**
     * Test that notification appears for a service YAML file without schema hint
     */
    public void testNotificationAppearsForServiceFileWithoutSchemaHint() {
        VirtualFile file = myFixture.addFileToProject(
            "config/services.yaml",
            "services:\n" +
            "    App\\Service\\MyService:\n" +
            "        arguments: ['@doctrine']"
        ).getVirtualFile();

        JComponent panel = getEditorNotificationPanel(file);
        assertNotNull("Notification should appear for service file without schema hint", panel);
        assertTrue("Should be EditorNotificationPanel", panel instanceof EditorNotificationPanel);

        EditorNotificationPanel notificationPanel = (EditorNotificationPanel) panel;
        assertTrue(
            "Panel text should mention schema hint",
            notificationPanel.getText().contains("YAML schema hint")
        );
    }

    /**
     * Test that notification does NOT appear when schema hint is already present
     */
    public void testNotificationDoesNotAppearWhenSchemaHintExists() {
        VirtualFile file = myFixture.addFileToProject(
            "config/services.yaml",
            "# yaml-language-server: $schema=../vendor/symfony/dependency-injection/Loader/schema/services.schema.json\n" +
            "services:\n" +
            "    App\\Service\\MyService:\n" +
            "        arguments: ['@doctrine']"
        ).getVirtualFile();

        JComponent panel = getEditorNotificationPanel(file);
        assertNull("Notification should NOT appear when schema hint exists", panel);
    }

    /**
     * Test that notification does NOT appear for non-service YAML files
     */
    public void testNotificationDoesNotAppearForNonServiceFiles() {
        VirtualFile file = myFixture.addFileToProject(
            "config/routes.yaml",
            "app_homepage:\n" +
            "    path: /\n" +
            "    controller: App\\Controller\\DefaultController::index"
        ).getVirtualFile();

        JComponent panel = getEditorNotificationPanel(file);
        assertNull("Notification should NOT appear for non-service YAML files", panel);
    }

    /**
     * Test that schema hint detection works even when comment is nested (not at root level)
     */
    public void testSchemaHintDetectionWithNestedComment() {
        VirtualFile file = myFixture.addFileToProject(
            "config/services.yaml",
            "# Some header comment\n" +
            "# yaml-language-server: $schema=../vendor/symfony/dependency-injection/Loader/schema/services.schema.json\n" +
            "services:\n" +
            "    App\\Service\\MyService: ~"
        ).getVirtualFile();

        JComponent panel = getEditorNotificationPanel(file);
        assertNull("Notification should NOT appear when schema hint exists on second line", panel);
    }

    /**
     * Test that schema hint detection is flexible with formatting
     */
    public void testSchemaHintDetectionWithVariousFormats() {
        // Test with spaces
        VirtualFile file1 = myFixture.addFileToProject(
            "config/services1.yaml",
            "#   yaml-language-server:   $schema=../vendor/symfony/dependency-injection/Loader/schema/services.schema.json\n" +
            "services:\n" +
            "    App\\Service\\MyService: ~"
        ).getVirtualFile();

        assertNull("Should detect schema hint with extra spaces", getEditorNotificationPanel(file1));

        // Test without spaces
        VirtualFile file2 = myFixture.addFileToProject(
            "config/services2.yaml",
            "#yaml-language-server:$schema=../vendor/symfony/dependency-injection/Loader/schema/services.schema.json\n" +
            "services:\n" +
            "    App\\Service\\MyService: ~"
        ).getVirtualFile();

        assertNull("Should detect schema hint without spaces", getEditorNotificationPanel(file2));
    }

    /**
     * Test that notification appears for files in config/services directory
     */
    public void testNotificationAppearsForServicesDirectory() {
        VirtualFile file = myFixture.addFileToProject(
            "config/services/my_services.yaml",
            "services:\n" +
            "    App\\Custom\\Service: ~"
        ).getVirtualFile();

        JComponent panel = getEditorNotificationPanel(file);
        assertNotNull("Notification should appear for files in services directory", panel);
    }

    /**
     * Helper method to get the editor notification panel for a file
     */
    private JComponent getEditorNotificationPanel(VirtualFile file) {
        FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(getProject());
        FileEditor fileEditor = fileEditorManager.openFile(file, true)[0];

        YamlSchemaEditorNotificationProvider provider = new YamlSchemaEditorNotificationProvider();
        var function = provider.collectNotificationData(getProject(), file);

        if (function == null) {
            return null;
        }

        return function.apply(fileEditor);
    }
}
