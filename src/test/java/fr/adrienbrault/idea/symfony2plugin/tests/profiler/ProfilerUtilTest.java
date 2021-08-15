package fr.adrienbrault.idea.symfony2plugin.tests.profiler;

import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.LocalProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;
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
     * @see ProfilerUtil#getBaseProfilerUrlFromRequest
     */
    public void testGetBaseProfilerUrlFromRequest() {
        assertEquals("http://127.0.0.1", ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1"));
        assertEquals("http://127.0.0.1", ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1:80"));
        assertEquals("http://127.0.0.1:8080", ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1:8080"));
        assertEquals("http://127.0.0.1:8080", ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1:8080/"));
        assertEquals("http://127.0.0.1:8080", ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1:8080/app_dev.php"));

        assertEquals(
            "http://127.0.0.1:8080/app_dev.php",
            ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1:8080/app_dev.php/")
        );

        assertEquals(
            "http://127.0.0.1:8080/app/app_dev.php",
            ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1:8080/app/app_dev.php/")
        );

        assertEquals(
            "http://127.0.0.1:8080/app/app_stage.php",
            ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1:8080/app/app_stage.php/")
        );

        assertEquals(
            "http://127.0.0.1:8080/app/app_test.php",
            ProfilerUtil.getBaseProfilerUrlFromRequest("http://127.0.0.1:8080/app/app_test.php/")
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
}
