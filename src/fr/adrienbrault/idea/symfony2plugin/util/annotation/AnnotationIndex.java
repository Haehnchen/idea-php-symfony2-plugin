package fr.adrienbrault.idea.symfony2plugin.util.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class AnnotationIndex {

    public static HashMap<String, AnnotationConfig> getControllerAnnotations() {

        HashMap<String, AnnotationConfig> controllerAnnotations = new HashMap<String, AnnotationConfig>();

        controllerAnnotations.put("@Template", new AnnotationConfig("@Template", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template"));
        controllerAnnotations.put("@Method", new AnnotationConfig("@Template", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Method"));

        controllerAnnotations.put("@Cache", new AnnotationConfig("@Template", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache")
            .insertValue("expires", "smaxage", "maxage")
            .insertValue(AnnotationValue.Type.Array, "vary")
        );

        controllerAnnotations.put("@Route", new AnnotationConfig("@Template", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route")
            .insertValue("service")
            .insertValue(AnnotationValue.Type.Array, "requirements", "defaults")
        );

        controllerAnnotations.put("@ParamConverter", new AnnotationConfig("@Template", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\ParamConverter")
            .insertValue("class")
            .insertValue(AnnotationValue.Type.Array, "options")
        );

        return controllerAnnotations;
    }

}
