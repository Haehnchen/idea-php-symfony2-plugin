package fr.adrienbrault.idea.symfony2plugin.tests.profiler;

import fr.adrienbrault.idea.symfony2plugin.profiler.HttpProfilerIndex;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.profiler.HttpProfilerIndex
 */
public class HttpProfilerIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testGetUrlForRequest() {
        HttpProfilerIndex index = new HttpProfilerIndex(getProject(), "http://www.google.de");

        assertEquals(
            "http://foobar.de",
            index.getUrlForRequest(new MyProfilerUrlRequestInterface("http://foobar.de"))
        );

        assertEquals(
            "http://www.google.de/foobar",
            index.getUrlForRequest(new MyProfilerUrlRequestInterface("foobar"))
        );
    }

    private static class MyProfilerUrlRequestInterface implements ProfilerRequestInterface {
        @NotNull
        private final String profilerUrl;

        MyProfilerUrlRequestInterface(@NotNull String profilerUrl) {
            this.profilerUrl = profilerUrl;
        }

        @NotNull
        @Override
        public String getHash() {
            return "";
        }

        @Nullable
        @Override
        public String getMethod() {
            return "";
        }

        @NotNull
        @Override
        public String getUrl() {
            return "";
        }

        @NotNull
        @Override
        public String getProfilerUrl() {
            return this.profilerUrl;
        }

        @Override
        public int getStatusCode() {
            return 0;
        }

        @Nullable
        @Override
        public <T> T getCollector(Class<T> classFactory) {
            return null;
        }
    }
}
