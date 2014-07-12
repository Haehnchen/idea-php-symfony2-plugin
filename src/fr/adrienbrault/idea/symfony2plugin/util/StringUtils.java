package fr.adrienbrault.idea.symfony2plugin.util;

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

}
