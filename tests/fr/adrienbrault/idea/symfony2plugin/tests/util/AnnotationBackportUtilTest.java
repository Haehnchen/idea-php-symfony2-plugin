package fr.adrienbrault.idea.symfony2plugin.tests.util;

import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import org.junit.Assert;
import org.junit.Test;



public class AnnotationBackportUtilTest extends Assert {

    @Test
    public void testAnnotationRouteName() {
        assertEquals("my_page_so_good", AnnotationBackportUtil.getAnnotationRouteName("\"/my/page/so-good\", name=\"my_page_so_good\""));
        assertEquals("my.page.so.good", AnnotationBackportUtil.getAnnotationRouteName("\"/my/page/so-good\", name=\"my.page.so.good\""));
        assertEquals("my-page.so_good", AnnotationBackportUtil.getAnnotationRouteName("\"/my/page/so-good\", name=\"my-page.so_good\""));
    }

}
