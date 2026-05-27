package fr.adrienbrault.idea.symfony2plugin.tests.profiler;

import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.profiler.LocalProfilerIndex;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.HttpProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.LocalProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ProfilerUtil
 */
public class ProfilerUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/profiler/fixtures";
    }

    /**
     * @see ProfilerUtil#createRequestsFromIndexHtml
     */
    public void testCreateRequestsFromIndexHtml() {
        PsiFile psiFile = myFixture.configureByFile("profiler-index.html");
        Collection<ProfilerRequestInterface> requests = ProfilerUtil.createRequestsFromIndexHtml(getProject(), psiFile.getText(), "http://127.0.0.1:8000/");

        ProfilerRequestInterface request = requests.iterator().next();

        assertEquals("a9eaab", request.getHash());
        assertEquals("GET", request.getMethod());
        assertEquals("http://127.0.0.1:8000/_profiler/search/results?ip=&amp;limit=10", request.getUrl());
        assertEquals("http://127.0.0.1:8000/_profiler/a9eaab", request.getProfilerUrl());
        assertEquals(404, request.getStatusCode());
    }

    /**
     * @see ProfilerUtil#createRequestsFromIndexHtml
     */
    public void testCreateRequestsFromIndexHtmlRemovesProfilerRoutePrefixFromTokenLinks() {
        PsiFile psiFile = myFixture.configureByFile("profiler-index-with-route-prefix.html");
        Collection<ProfilerRequestInterface> requests = ProfilerUtil.createRequestsFromIndexHtml(getProject(), psiFile.getText(), "http://127.0.0.1:8000/prefix/");

        ProfilerRequestInterface request = requests.iterator().next();

        assertEquals("a9eaab", request.getHash());
        assertEquals("GET", request.getMethod());
        assertEquals("http://127.0.0.1:8000/prefix/_profiler/search/results?ip=&amp;limit=10", request.getUrl());
        assertEquals("http://127.0.0.1:8000/prefix/_profiler/a9eaab", request.getProfilerUrl());
        assertEquals(404, request.getStatusCode());
    }

    /**
     * @see ProfilerUtil#getRequestAttributes
     */
    public void testGetRequestValues() {
        PsiFile psiFile = myFixture.configureByFile("profiler-request.html");
        Map<String, String> requests = ProfilerUtil.getRequestAttributes(getProject(), psiFile.getText());

        assertEquals("my.controller:latestAction", requests.get("_controller"));
        assertEquals("foo_route", requests.get("_route"));
    }

    /**
     * @see ProfilerUtil#getRenderedElementTwigTemplates
     */
    public void testGetRenderedElementTwigTemplates() {
        PsiFile psiFile = myFixture.configureByFile("profiler-twig.html");
        Map<String, Integer> requests = ProfilerUtil.getRenderedElementTwigTemplates(getProject(), psiFile.getText());

        assertEquals(16, requests.get("@Twig/Exception/trace.html.twig").intValue());
        assertEquals(1, requests.get("@Twig/Exception/traces.html.twig").intValue());
    }

    /**
     * @see ProfilerUtil#getRenderedElementTwigTemplateNames
     */
    public void testGetRenderedElementTwigTemplateNamesPreservesProfilerOrder() {
        PsiFile psiFile = myFixture.configureByFile("profiler-twig.html");
        List<String> templates = ProfilerUtil.getRenderedElementTwigTemplateNames(getProject(), psiFile.getText());

        assertEquals(Arrays.asList(
            "@Twig/Exception/exception_full.html.twig",
            "@Twig/layout.html.twig",
            "@Twig/Exception/exception.html.twig"
        ), templates.subList(0, 3));
    }

    /**
     * @see ProfilerUtil#getBaseProfilerUrlFromRequest
     */
    public void testGetBaseProfilerUrlFromRequest() {
        assertEquals("http://127.0.0.1", ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1", "", "")));
        assertEquals("http://127.0.0.1", ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1:80", "", "")));
        assertEquals("http://127.0.0.1:8080", ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1:8080", "", "")));
        assertEquals("http://127.0.0.1:8080", ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1:8080/", "", "")));
        assertEquals("http://127.0.0.1:8080", ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1:8080/app_dev.php", "", "")));

        assertEquals(
            "http://127.0.0.1:8080/app_dev.php",
            ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1:8080/app_dev.php/", "", ""))
        );

        assertEquals(
            "http://127.0.0.1:8080/app/app_dev.php",
            ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1:8080/app/app_dev.php/", "", ""))
        );

        assertEquals(
            "http://127.0.0.1:8080/app/app_stage.php",
            ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1:8080/app/app_stage.php/", "", ""))
        );

        assertEquals(
            "http://127.0.0.1:8080/app/app_test.php",
            ProfilerUtil.getBaseProfilerUrlFromRequest(new HttpProfilerRequest(0, "aaa", "http://127.0.0.1:8080/app/app_test.php/", "", ""))
        );
    }

    /**
     * @see ProfilerUtil#formatProfilerRow
     */
    public void testFormatProfilerRow() {
        assertEquals(
            "(200) /foobar",
            ProfilerUtil.formatProfilerRow(new LocalProfilerRequest("18e6b8,127.0.0.1,GET,http://127.0.0.1:8000/foobar,1474185112,76c8ab,200".split(",")))
        );

        assertEquals(
            "(200) /foobar",
            ProfilerUtil.formatProfilerRow(new LocalProfilerRequest("18e6b8,127.0.0.1,GET,http://127.0.0.1:8000/app_dev.php/foobar,1474185112,76c8ab,200".split(",")))
        );

        assertEquals(
            "(404) /foobar/foobar/foobar/foobar/foo...",
            ProfilerUtil.formatProfilerRow(new LocalProfilerRequest("18e6b8,127.0.0.1,GET,http://127.0.0.1:8000/app_test.php/foobar/foobar/foobar/foobar/foobar/,1474185112,76c8ab,404".split(",")))
        );

        assertEquals(
            "(404) asdss127.0.0.1:8000/app_test.php...",
            ProfilerUtil.formatProfilerRow(new LocalProfilerRequest("18e6b8,127.0.0.1,GET,asdss127.0.0.1:8000/app_test.php/foobar/foobar/,1474185112,76c8ab,404".split(",")))
        );
    }

    public void testGetContentForFile() {
        String contentFor = ProfilerUtil.getContentForFile(new File(this.getTestDataPath() + "/748f72-gzip-profiler"));
        assertTrue(contentFor.startsWith("a:9"));

        String contentFor2 = ProfilerUtil.getContentForFile(new File(this.getTestDataPath() + "/748f72-gzip-profiler-raw"));
        assertTrue(contentFor2.startsWith("a:9"));
    }

    public void testGetContentForFileRejectsLargeFiles() throws Exception {
        Path file = Files.createTempFile("symfony-profiler-large", ".data");
        Files.write(file, new byte[10 * 1024 * 1024 + 1]);

        assertNull(ProfilerUtil.getContentForFile(file.toFile()));
    }

    public void testNormalizeHttpProfilerBaseUrlAcceptsHttpUrls() {
        assertEquals("http://127.0.0.1:8000", ProfilerUtil.normalizeHttpProfilerBaseUrl("http://127.0.0.1:8000/"));
        assertEquals("http://symfony.localhost:8000", ProfilerUtil.normalizeHttpProfilerBaseUrl("http://symfony.localhost:8000/"));
        assertEquals("http://symfony:8000", ProfilerUtil.normalizeHttpProfilerBaseUrl("http://symfony:8000/"));
        assertEquals("https://example.com/profiler", ProfilerUtil.normalizeHttpProfilerBaseUrl("https://example.com/profiler/"));
        assertNull(ProfilerUtil.normalizeHttpProfilerBaseUrl("file:///tmp/profiler"));
        assertNull(ProfilerUtil.normalizeHttpProfilerBaseUrl("http://user:pass@localhost:8000"));
    }

    public void testLocalProfilerIndexDecoratesValidProfilerHashOnly() throws Exception {
        Path profilerDir = Files.createTempDirectory("symfony-profiler");
        Path index = profilerDir.resolve("index.csv");
        Files.writeString(index, "18e6b8,127.0.0.1,GET,http://127.0.0.1:8000/foobar,1474185112,76c8ab,200\n", StandardCharsets.UTF_8);

        Path dataFile = profilerDir.resolve("b8").resolve("e6").resolve("18e6b8");
        Files.createDirectories(dataFile.getParent());
        Files.writeString(dataFile, "a:0:{}", StandardCharsets.UTF_8);

        List<ProfilerRequestInterface> requests = new LocalProfilerIndex(index.toFile()).getRequests();

        assertEquals(1, requests.size());
        assertNotNull(requests.getFirst().getCollector(DefaultDataCollectorInterface.class));
    }

    public void testLocalProfilerIndexDoesNotReadInvalidProfilerHash() throws Exception {
        Path profilerDir = Files.createTempDirectory("symfony-profiler");
        Path index = profilerDir.resolve("index.csv");
        Files.writeString(index, "../secret,127.0.0.1,GET,http://127.0.0.1:8000/foobar,1474185112,76c8ab,200\n", StandardCharsets.UTF_8);

        List<ProfilerRequestInterface> requests = new LocalProfilerIndex(index.toFile()).getRequests();

        assertEquals(1, requests.size());
        assertNull(requests.getFirst().getCollector(DefaultDataCollectorInterface.class));
    }
}
