package fr.adrienbrault.idea.symfony2plugin.tests.stubs;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

import java.io.IOException;
import java.util.List;

public class ContainerAutoconfigurationExclusionTest extends SymfonyLightCodeInsightFixtureTestCase {
    private static final String[] EXCLUDED_SERVICES = {
        "App\\Customer", "App\\Order", "App\\Address", "App\\BaseModel", "App\\Message",
        "App\\StreamableData", "App\\CustomAttribute", "App\\ApplicationTest", "App\\CompilerPass",
        "App\\ValidationConstraint", "App\\Status"
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("autoconfiguration_excluded_classes.php", "src/Domain.php");
    }

    @Override
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/fixtures";
    }

    public void testSymfony73ExcludesAutoconfiguredClasses() {
        configure("7.3.0", true, "");

        assertExcluded(EXCLUDED_SERVICES);
        assertPresent("App\\Service", "App\\AnnotationEntity");

        // This is the guard used by AutowireWrongClassInspection and the other constructor inspections.
        assertFalse(ServiceUtil.isPhpClassAService(PhpElementsUtil.getClass(getProject(), "App\\Order")));
    }

    public void testSymfony8ExcludesAutoconfiguredClasses() {
        configure("8.0.0", true, "");
        assertExcluded(EXCLUDED_SERVICES);
    }

    public void testSymfony72KeepsClasses() {
        configure("7.2.9", true, "");
        assertPresent(EXCLUDED_SERVICES);
    }

    public void testUnknownSymfonyVersionKeepsClasses() {
        configure(null, true, "");
        assertPresent(EXCLUDED_SERVICES);
    }

    public void testDisabledAutoconfigurationKeepsClasses() {
        configure("7.3.0", false, "");
        assertPresent(EXCLUDED_SERVICES);
    }

    public void testExplicitAutoconfigurationOverrideKeepsEntity() {
        configure("7.3.0", true, """
                App\\Order:
                    autoconfigure: false
            """);

        assertPresent("App\\Order");
        assertExcluded("App\\Customer");
    }

    public void testNamedServiceAndAliasOfExcludedEntityAreExcluded() {
        configure("7.3.0", true, """
                app.order:
                    class: App\\Order
                app.order_alias:
                    alias: App\\Order
            """);

        assertExcluded("App\\Order", "app.order", "app.order_alias");
    }

    public void testNamedServiceWithoutAutoconfigurationIsKept() {
        configure("7.3.0", true, """
                app.order:
                    class: App\\Order
                    autoconfigure: false
            """);

        assertPresent("app.order");
        assertExcluded("App\\Order");
        assertContainsElements(
            ContainerCollectionResolver.ServiceCollector.create(getProject()).convertClassNameToServices("App\\Order"),
            "app.order"
        );
    }

    public void testIgnoringAttributesKeepsEntityButStillExcludesTestCase() {
        configure("7.3.0", true, """
                App\\Order:
                    tags: [container.ignore_attributes]
                App\\ApplicationTest:
                    tags: [container.ignore_attributes]
            """);

        assertPresent("App\\Order");
        assertExcluded("App\\ApplicationTest", "App\\Customer");
    }

    public void testChangingInheritanceInvalidatesExclusions() {
        configure("7.3.0", true, "");
        PsiFile file = myFixture.addFileToProject("src/ChangingService.php", "<?php namespace App; class ChangingService {}");
        assertPresent("App\\ChangingService");

        replaceFileText(file, "<?php namespace App; class ChangingService extends \\PHPUnit\\Framework\\TestCase {}");
        assertExcluded("App\\ChangingService");
    }

    public void testChangingAttributesInvalidatesExclusions() {
        configure("7.3.0", true, "");
        String plainClass = "<?php namespace App; class ChangingService {}";
        PsiFile file = myFixture.addFileToProject("src/ChangingService.php", plainClass);
        assertPresent("App\\ChangingService");

        replaceFileText(file, "<?php namespace App; #[\\Doctrine\\ORM\\Mapping\\Entity] class ChangingService {}");
        assertExcluded("App\\ChangingService");

        replaceFileText(file, plainClass);
        assertPresent("App\\ChangingService");
    }

    public void testChangingSymfonyVersionInvalidatesExclusions() {
        configure("7.2.9", true, "");
        PsiFile kernel = myFixture.getPsiManager().findFile(myFixture.findFileInTempDir("Kernel.php"));
        assertNotNull(kernel);
        assertPresent(EXCLUDED_SERVICES);

        replaceFileText(kernel, "<?php namespace Symfony\\Component\\HttpKernel; abstract class Kernel { const VERSION = '7.3.0'; }");
        assertExcluded(EXCLUDED_SERVICES);

        replaceFileText(kernel, "<?php namespace Symfony\\Component\\HttpKernel; abstract class Kernel { const VERSION = '7.2.9'; }");
        assertPresent(EXCLUDED_SERVICES);
    }

    public void testCompiledContainerTakesPrecedenceOverAutomaticExclusion() throws IOException {
        assertCompiledContainerExclusion(false);
    }

    public void testExcludedDefinitionInCompiledContainerIsExcluded() throws IOException {
        assertCompiledContainerExclusion(true);
    }

    private void assertCompiledContainerExclusion(boolean excluded) throws IOException {
        configure("7.3.0", true, "");
        String containerPath = "var/cache/dev/" + getTestName(false) + "App_KernelDevDebugContainer.xml";
        createFileInProjectRoot(containerPath, """
            <?xml version="1.0" encoding="UTF-8"?>
            <container>
                <service id="App\\Order" class="App\\Order">
                    %s
                </service>
            </container>
            """.formatted(excluded ? "<tag name=\"container.excluded\"/>" : ""));
        Settings settings = Settings.getInstance(getProject());
        List<ContainerFile> previousContainerFiles = settings.containerFiles;
        try {
            settings.containerFiles = List.of(new ContainerFile(containerPath));
            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();

            if (excluded) {
                assertExcluded("App\\Order");
            } else {
                assertPresent("App\\Order");
            }
            assertExcluded("App\\Customer");
        } finally {
            settings.containerFiles = previousContainerFiles;
            VirtualFile compiledFile = getProject().getBaseDir().findFileByRelativePath(containerPath);
            if (compiledFile != null) {
                WriteCommandAction.writeCommandAction(getProject()).run(() -> compiledFile.delete(this));
            }
            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();
        }
    }

    private void replaceFileText(PsiFile file, String text) {
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getProject());
            var document = documentManager.getDocument(file);
            assertNotNull(document);
            document.setText(text);
            documentManager.commitDocument(document);
        });
    }

    private void configure(String version, boolean autoconfigure, String definitions) {
        if (version != null) {
            myFixture.addFileToProject("Kernel.php", "<?php namespace Symfony\\Component\\HttpKernel; abstract class Kernel { const VERSION = '" + version + "'; }");
        }
        myFixture.addFileToProject("config/services.yml", """
            services:
                _defaults:
                    autowire: true
                    autoconfigure: %s
                App\\:
                    resource: '../src/'
            """.formatted(autoconfigure) + definitions);
    }

    private void assertExcluded(String... services) {
        // Query names first, as completion may use this path without resolving services beforehand.
        assertDoesntContain(ContainerCollectionResolver.getServiceNames(getProject()), services);
        for (String service : services) {
            assertNull(service, ContainerCollectionResolver.getService(getProject(), service));
        }
    }

    private void assertPresent(String... services) {
        assertContainsElements(ContainerCollectionResolver.getServiceNames(getProject()), services);
        for (String service : services) {
            assertNotNull(service, ContainerCollectionResolver.getService(getProject(), service));
        }
    }
}
