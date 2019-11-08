package fr.adrienbrault.idea.symfonyplugin.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StringUtils {

    public static String camelize(String input) {
        return camelize(input, false);
    }

    public static String camelize(String input, boolean startWithLowerCase) {

        String[] strings = org.apache.commons.lang.StringUtils.split(input.toLowerCase(), "_");
        for (int i = startWithLowerCase ? 1 : 0; i < strings.length; i++){
            strings[i] = org.apache.commons.lang.StringUtils.capitalize(strings[i]);
        }

        input = org.apache.commons.lang.StringUtils.join(strings);

        if(!startWithLowerCase) {
            return ucfirst(input);
        }

        return input;
    }

    public static String underscore(String camelCasedWord) {
        return org.apache.commons.lang.StringUtils.capitalize(camelCasedWord).replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    public static String ucfirst(String chaine){
        if(chaine.length() < 1) {
            return chaine;
        }
        return chaine.substring(0, 1).toUpperCase() + chaine.substring(1);
    }

    @NotNull
    public static String lcfirst(@NotNull String input){
        if(input.length() < 1) {
            return input;
        }
        
        return Character.toLowerCase(input.charAt(0)) +
            (input.length() > 1 ? input.substring(1) : "");
    }

    /**
     * Simple string compare if class name is in same namespace
     * Starting backslash doesnt break equal check
     *
     * @param class1 \Foo\Class
     * @param class2 \Foo or Foo
     */
    public static boolean startWithEqualClassname(String class1, String class2) {
        if(class1.startsWith("\\")) {
            class1 = class1.substring(1);
        }

        if(class2.startsWith("\\")) {
            class2 = class2.substring(1);
        }

        return class1.startsWith(class2);
    }

    /**
     * Test for interpolated string "#{segment.typeKey}.html.twig"
     */
    public static boolean isInterpolatedString(@NotNull String content) {
        return content.matches(".*#\\{[^{]*}.*");
    }
}
