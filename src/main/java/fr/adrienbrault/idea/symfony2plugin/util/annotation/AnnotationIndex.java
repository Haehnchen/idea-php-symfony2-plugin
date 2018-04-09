package fr.adrienbrault.idea.symfony2plugin.util.annotation;

import java.util.HashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationIndex {

    public static HashMap<String, AnnotationConfig> getControllerAnnotations() {

        HashMap<String, AnnotationConfig> controllerAnnotations = new HashMap<>();

        controllerAnnotations.put("@Template", new AnnotationConfig("@Template", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template"));
        controllerAnnotations.put("@Method", new AnnotationConfig("@Method", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Method"));

        controllerAnnotations.put("@Cache", new AnnotationConfig("@Cache", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache")
            .insertValue("expires", "smaxage", "maxage")
            .insertValue(AnnotationValue.Type.Array, "vary")
        );

        controllerAnnotations.put("@Route", new AnnotationConfig("@Route", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route")
            .insertValue("service")
            .insertValue(AnnotationValue.Type.Array, "requirements", "defaults")
        );

        controllerAnnotations.put("@ParamConverter", new AnnotationConfig("@ParamConverter", "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\ParamConverter")
            .insertValue("class")
            .insertValue(AnnotationValue.Type.Array, "options")
        );

        return controllerAnnotations;
    }

}
