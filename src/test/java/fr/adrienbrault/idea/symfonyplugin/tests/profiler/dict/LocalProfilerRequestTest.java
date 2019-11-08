package fr.adrienbrault.idea.symfonyplugin.tests.profiler.dict;

import fr.adrienbrault.idea.symfonyplugin.profiler.dict.LocalProfilerRequest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalProfilerRequestTest extends Assert {

    @Test
    public void testThatCsvItemIsMapped() {
        LocalProfilerRequest request = new LocalProfilerRequest("18e6b8,127.0.0.1,GET,http://127.0.0.1:8000/foobar,1474185112,76c8ab,200".split(","));

        assertEquals("18e6b8", request.getHash());
        assertEquals("GET", request.getMethod());
        assertEquals("http://127.0.0.1:8000/foobar", request.getUrl());
        assertEquals("_profiler/18e6b8", request.getProfilerUrl());
        assertEquals(200, request.getStatusCode());
    }

    @Test
    public void testThatCsvItemIsMappedWithStatusOnOldSymfony() {
        // Symfony 2.x
        LocalProfilerRequest request = new LocalProfilerRequest("18e6b8,127.0.0.1,GET,http://127.0.0.1:8000/foobar,1474185112,76c8ab".split(","));
        assertEquals(0, request.getStatusCode());

        // minimum split
        request = new LocalProfilerRequest("18e6b8,127.0.0.1,GET,http://127.0.0.1:8000/foobar".split(","));
        assertEquals(0, request.getStatusCode());
        assertEquals("http://127.0.0.1:8000/foobar", request.getUrl());
    }
}
