package fr.adrienbrault.idea.symfony2plugin.tests.routing;


import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class RouteHelperTest extends Assert {

    @Test
    public void testParse() throws Exception {
        File testFile = new File(this.getClass().getResource("appTestUrlGenerator.php").getFile());

        // @TODO: re-implement test on PsiElements
        //Map<String, Route> routes = RouteHelper.getRoutes(fileToString(testFile.getPath()));
        //assertEquals("Lol\\CoreBundle\\Controller\\FeedbackController::feedbackAction", routes.get("feedback").getController());
        //assertEquals("Lol\\ApiBundle\\Controller\\UsersController::getInfoAction", routes.get("api_users_getInfo").getController());
        //assertNull(routes.get("ru__RG__page"));
        //assertNull(routes.get("_assetic_91dd2a8"));
    }

}
