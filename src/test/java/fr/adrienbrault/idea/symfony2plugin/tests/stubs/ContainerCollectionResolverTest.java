package fr.adrienbrault.idea.symfony2plugin.tests.stubs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ClassServiceDefinitionTargetLazyValue;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerServiceMetadata;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
 */
public class ContainerCollectionResolverTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText("foo1.yml", "" +
            "parameters:\n" +
            "    bar: foo\n" +
            "\n" +
            "services:\n" +
            "    foo:\n" +
            "        class: DateTime\n" +
            "    foo_slash:\n" +
            "        class: \\DateTime\n" +
            "    foo_UPPER:\n" +
            "        class: \\DateTime\n" +
            "    foo_datetime:\n" +
            "        class: \\DateTime\n"
        );

        myFixture.configureByText("foo2.yml", "" +
            "services:\n" +
            "    foo_datetime:\n" +
            "        class: \\DateTimeInterface\n"
        );

        myFixture.configureByText("foo3.yml", "" +
            "services:\n" +
            "    foo_datetime:\n" +
            "        class: DateTimeInterface\n"
        );

        myFixture.configureByText("foo4.yml", "" +
            "parameters:\n" +
            "    bar: foo\n" +
            "\n" +
            "services:\n" +
            "    foo_datetime:\n" +
            "        class: %bar%\n"
        );

        myFixture.copyFileToProject("ContainerBuilder.php");
        myFixture.copyFileToProject("decorator.services.xml");
        myFixture.copyFileToProject("kernel_parameter.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/fixtures";
    }

    public void testCaseInsensitiveService() {
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo").getClassName());
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo_upper").getClassName());
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "Foo").getClassName());
        assertNotNull(ContainerCollectionResolver.getService(getProject(), "foo"));
        assertNotNull(ContainerCollectionResolver.getService(getProject(), "Foo"));
    }

    public void testCaseInsensitiveParameter() {
        assertTrue(ContainerCollectionResolver.getParameterNames(getProject()).contains("Bar"));
        assertTrue(ContainerCollectionResolver.getParameterNames(getProject()).contains("bar"));
    }

    public void testThatLeadingSlashIsStripped() {
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo_slash").getClassName());
    }

    public void testThatDuplicateClassNamesProvidesVariantsAndResolvesParameter() {
        ContainerService results = ContainerCollectionResolver.getService(getProject(), "foo_datetime");
        Set<String> classNames = results.getClassNames();
        assertSize(3, classNames);

        assertTrue(classNames.contains("DateTime"));
        assertTrue(classNames.contains("DateTimeInterface"));
        assertTrue(classNames.contains("foo"));
    }

    public void testThatAliasedServiceIsEqualWithMainService() {

        myFixture.configureByText(YAMLFileType.YML, "" +
                "services:\n" +
                "    foo_as_alias:\n" +
                "        alias: foo\n" +
                "    foo:\n" +
                "        class: DateTime\n"
        );

        assertNotNull(ContainerCollectionResolver.getService(getProject(), "foo_as_alias"));
        assertNotNull(ContainerCollectionResolver.getService(getProject(), "foo"));
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo_as_alias").getClassName());
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo").getClassName());
    }

    public void testThatContainerBuilderParameterAreCollected() {
        assertContainsElements(ContainerCollectionResolver.getParameterNames(getProject()), "container.builder.parameter");
        ContainerParameter containerParameter = ContainerCollectionResolver.getParameters(getProject()).get("container.builder.parameter");
        assertNotNull(containerParameter);
        assertTrue(containerParameter.isWeak());
    }

    public void testThatDecoratedServiceProvidesInner() {
        ContainerService service = ContainerCollectionResolver.getService(getProject(), "espend.my_next_foo.inner");
        assertNotNull(service);

        assertEquals("espend\\MyFirstFoo", service.getClassName());
        assertEquals("espend.my_next_foo.inner", service.getName());
        assertTrue(service.isPrivate());

        service = ContainerCollectionResolver.getService(getProject(), "espend.my_bar_customer_inner.inner_foo");
        assertNotNull(service);

        assertEquals("espend\\MyNextFoo", service.getClassName());
        assertEquals("espend.my_bar_customer_inner.inner_foo", service.getName());

        assertEquals(true, service.isPrivate());
        assertEquals(true, service.isWeak());
    }

    public void testThatGetKernelParametersAreCollected() {
        assertContainsElements(ContainerCollectionResolver.getParameters(getProject()).keySet(), "kernel.foobar");
    }

    public void testThatResourceBasedServicesAreResolved() {
        myFixture.copyFileToProject("ResourceFooService.php", "Service/ResourceFooService.php");
        VirtualFile virtualFile = myFixture.copyFileToProject("resource_based_services.yml", "config/services.yml");
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(virtualFile);
        assertNotNull(psiFile);

        assertNotNull(ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService"));

        ContainerService service = ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService");
        assertNotNull(service);
        assertTrue(service.isAutowireEnabled());
        ContainerServiceMetadata metadata = getResourcePrototypeMetadata(service, "App\\Service\\");
        assertTrue(metadata.autoconfigure());
        assertContainsElements(metadata.resource(), "../Service/*");
        assertEmpty(metadata.exclude());

        Collection<? extends PsiElement> targets = new ClassServiceDefinitionTargetLazyValue(getProject(), "\\App\\Service\\ResourceFooService").get();
        assertTrue(targets.stream().anyMatch(target ->
            psiFile.equals(target.getContainingFile()) && target.getText().contains("App\\Service\\")
        ));
    }

    public void testThatResourceBasedServiceWithExcludeAttributeIsFiltered() {
        myFixture.copyFileToProject("ExcludedService.php", "Service/ExcludedService.php");
        myFixture.copyFileToProject("resource_based_services.yml", "config/services.yml");

        assertNull(ContainerCollectionResolver.getService(getProject(), "App\\Service\\ExcludedService"));
        assertNull(ContainerCollectionResolver.getService(getProject(), "App\\Service\\ExcludedService"));
    }

    public void testThatPhpArrayResourceBasedServicesAreResolved() {
        myFixture.copyFileToProject("ResourceFooService.php", "Service/ResourceFooService.php");
        myFixture.copyFileToProject("resource_based_services.php", "config/services.php");

        assertNotNull(ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService"));

        ContainerService service = ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService");
        assertNotNull(service);
        assertEquals("App\\Service\\ResourceFooService", service.getClassName());
        assertTrue(service.isWeak());
        assertTrue(service.isAutowireEnabled());
        ContainerServiceMetadata metadata = getResourcePrototypeMetadata(service, "App\\Service\\");
        assertTrue(metadata.autoconfigure());
        assertContainsElements(metadata.resource(), "../Service/*");
        assertEmpty(metadata.exclude());
    }

    public void testThatXmlResourceBasedServicesAreResolved() {
        myFixture.copyFileToProject("ResourceFooService.php", "Service/ResourceFooService.php");
        myFixture.copyFileToProject("resource_based_services.xml", "config/services.xml");

        assertNotNull(ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService"));

        ContainerService service = ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService");
        assertNotNull(service);
        assertTrue(service.isAutowireEnabled());
        ContainerServiceMetadata metadata = getResourcePrototypeMetadata(service, "App\\Service\\");
        assertTrue(metadata.autoconfigure());
        assertContainsElements(metadata.resource(), "../Service/*");
        assertEmpty(metadata.exclude());
    }

    public void testThatCompiledContainerMetadataIsMergedIntoExistingService() throws IOException {
        myFixture.copyFileToProject("ResourceFooService.php", "Service/ResourceFooService.php");
        myFixture.copyFileToProject("resource_based_services_merge.yml", "config/services.yml");
        String containerXml = new String(Files.readAllBytes(Paths.get(
            "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/fixtures/compiled_container_merge.xml"
        )));
        String containerPath = "var/cache/dev/" + getTestName(false) + "App_KernelDevDebugContainer.xml";
        createFileInProjectRoot(containerPath, containerXml);
        Settings settings = Settings.getInstance(getProject());
        List<ContainerFile> previousContainerFiles = settings.containerFiles;
        try {
            settings.containerFiles = List.of(new ContainerFile(containerPath));
            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();

            ContainerService merged = ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService");
            assertNotNull(merged);
            assertEquals("App\\Service\\ResourceFooService", merged.getClassName());
            assertTrue(merged.isAutowireEnabled());
            assertTrue(merged.isAutoconfigureEnabled());
            assertContainsElements(merged.getTags(), "resource_tag", "compiled_tag");
            assertEquals(2, merged.getMetadata().size());
            assertTrue(merged.getMetadata().stream().anyMatch(metadata -> metadata.sourceKind() == ContainerServiceMetadata.SourceKind.RESOURCE_PROTOTYPE));
            ContainerServiceMetadata compiledMetadata = merged.getMetadata().stream()
                .filter(metadata -> metadata.sourceKind() == ContainerServiceMetadata.SourceKind.COMPILED_CONTAINER)
                .findFirst()
                .orElseThrow();
            assertTrue(compiledMetadata.lazy());
            assertTrue(compiledMetadata.abstractDefinition());
            assertContainsElements(compiledMetadata.tags(), "compiled_tag");

            ContainerServiceMetadata resourceMetadata = getResourcePrototypeMetadata(merged, "App\\Service\\");
            assertTrue(resourceMetadata.autoconfigure());
            assertContainsElements(resourceMetadata.resource(), "../Service/*");
            assertContainsElements(resourceMetadata.tags(), "resource_tag");
        } finally {
            settings.containerFiles = previousContainerFiles != null ? previousContainerFiles : new ArrayList<>();

            VirtualFile compiledFile = getProject().getBaseDir().findFileByRelativePath(containerPath);
            if (compiledFile != null) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        compiledFile.delete(this);
                    } catch (IOException ignored) {
                    }
                });
            }

            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();
        }
    }

    public void testThatIndexedServiceMetadataIsMergedIntoExistingResourceService() {
        myFixture.copyFileToProject("ResourceFooService.php", "Service/ResourceFooService.php");
        myFixture.copyFileToProject("resource_based_services_merge.yml", "config/services.yml");
        myFixture.copyFileToProject("indexed_service_merge.yml", "config/services_merge.yml");

        ContainerService merged = ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService");
        assertNotNull(merged);
        assertEquals("App\\Service\\ResourceFooService", merged.getClassName());
        assertTrue(merged.isAutowireEnabled());
        assertTrue(merged.isAutoconfigureEnabled());
        assertContainsElements(merged.getTags(), "resource_tag", "indexed_tag");

        ContainerServiceMetadata resourceMetadata = getResourcePrototypeMetadata(merged, "App\\Service\\");
        assertTrue(resourceMetadata.autoconfigure());
        assertContainsElements(resourceMetadata.resource(), "../Service/*");
        assertContainsElements(resourceMetadata.tags(), "resource_tag");

        ContainerServiceMetadata indexedMetadata = getMetadataBySourceKind(merged, ContainerServiceMetadata.SourceKind.INDEXED_SERVICE);
        assertTrue(indexedMetadata.lazy());
        assertTrue(indexedMetadata.abstractDefinition());
        assertFalse(indexedMetadata.autowire());
        assertFalse(indexedMetadata.autoconfigure());
        assertContainsElements(indexedMetadata.tags(), "indexed_tag");
    }

    public void testThatScalarMetadataUsesIndexedPrecedenceAndPluralAccessorsKeepAllValues() throws IOException {
        myFixture.addFileToProject("config/services.yml", "" +
            "services:\n" +
            "    app.conflict:\n" +
            "        class: DateTime\n" +
            "        parent: indexed.parent\n" +
            "        decorates: indexed.decorates\n" +
            "        decoration_inner_name: indexed.inner\n"
        );

        String containerPath = "var/cache/dev/" + getTestName(false) + "App_KernelDevDebugContainer.xml";
        createFileInProjectRoot(containerPath, "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<container>\n" +
            "    <service id=\"app.conflict\"\n" +
            "             class=\"DateTime\"\n" +
            "             parent=\"compiled.parent\"\n" +
            "             decorates=\"compiled.decorates\"\n" +
            "             decoration-inner-name=\"compiled.inner\"\n" +
            "             deprecated=\"true\"/>\n" +
            "</container>\n"
        );
        Settings settings = Settings.getInstance(getProject());
        List<ContainerFile> previousContainerFiles = settings.containerFiles;
        try {
            settings.containerFiles = List.of(new ContainerFile(containerPath));
            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();

            ContainerService merged = ContainerCollectionResolver.getService(getProject(), "app.conflict");
            assertNotNull(merged);
            assertContainsElements(merged.getParents(), "indexed.parent", "compiled.parent");
            assertEquals("indexed.parent", new ArrayList<>(merged.getParents()).get(0));
            assertContainsElements(merged.getDecoratesValues(), "indexed.decorates", "compiled.decorates");
            assertEquals("indexed.decorates", new ArrayList<>(merged.getDecoratesValues()).get(0));
            assertContainsElements(merged.getDecorationInnerNames(), "indexed.inner", "compiled.inner");
            assertEquals("indexed.inner", new ArrayList<>(merged.getDecorationInnerNames()).get(0));
            assertFalse(merged.getMetadata().get(0).deprecated());
        } finally {
            settings.containerFiles = previousContainerFiles != null ? previousContainerFiles : new ArrayList<>();

            VirtualFile compiledFile = getProject().getBaseDir().findFileByRelativePath(containerPath);
            if (compiledFile != null) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        compiledFile.delete(this);
                    } catch (IOException ignored) {
                    }
                });
            }

            SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(getProject()).reloadConfiguration();
        }
    }

    public void testThatPhpArrayResourceBasedServicesRespectPerEntryAutowireOverride() {
        myFixture.copyFileToProject("ResourceFooService.php", "Service/ResourceFooService.php");
        myFixture.copyFileToProject("resource_based_services_autowire_false.php", "config/services.php");

        assertNull(ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService"));
        assertNull(ContainerCollectionResolver.getService(getProject(), "App\\Service\\ResourceFooService"));
    }

    public void testThatDirectPhpArrayResourceBasedServicesAreResolved() {
        myFixture.copyFileToProject("DirectResourceFooService.php", "DirectService/DirectResourceFooService.php");
        myFixture.copyFileToProject("direct_resource_based_services.php", "config/services.php");

        assertNotNull(ContainerCollectionResolver.getService(getProject(), "App\\DirectService\\DirectResourceFooService"));
        assertNotNull(ContainerCollectionResolver.getService(getProject(), "App\\DirectService\\DirectResourceFooService"));
    }

    public void testThatClosurePhpArrayResourceBasedServicesAreResolved() {
        myFixture.copyFileToProject("ClosureResourceFooService.php", "ClosureService/ClosureResourceFooService.php");
        myFixture.copyFileToProject("closure_resource_based_services.php", "config/services.php");

        assertNotNull(ContainerCollectionResolver.getService(getProject(), "App\\ClosureService\\ClosureResourceFooService"));
        assertNotNull(ContainerCollectionResolver.getService(getProject(), "App\\ClosureService\\ClosureResourceFooService"));
    }

    public void testThatExplicitServiceWithExcludeAttributeIsFilteredFromNames() {
        myFixture.copyFileToProject("ExcludedService.php");
        myFixture.configureByText("exclude_explicit.yml", "" +
            "services:\n" +
            "    App\\Service\\ExcludedService: ~\n"
        );

        assertNull(ContainerCollectionResolver.getService(getProject(), "App\\Service\\ExcludedService"));
    }

    @NotNull
    private static ContainerServiceMetadata getResourcePrototypeMetadata(@NotNull ContainerService service, @NotNull String resourceServiceId) {
        return service.getMetadata().stream()
            .filter(metadata -> Objects.equals(resourceServiceId, metadata.resourceServiceId()))
            .findFirst()
            .orElseThrow();
    }

    @NotNull
    private static ContainerServiceMetadata getMetadataBySourceKind(@NotNull ContainerService service, @NotNull ContainerServiceMetadata.SourceKind sourceKind) {
        return service.getMetadata().stream()
            .filter(metadata -> metadata.sourceKind() == sourceKind)
            .findFirst()
            .orElseThrow();
    }
}
