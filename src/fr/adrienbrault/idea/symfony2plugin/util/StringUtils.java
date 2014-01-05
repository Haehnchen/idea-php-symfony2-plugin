package fr.adrienbrault.idea.symfony2plugin.util;

public class StringUtils {

    public static String camelize(String value, boolean startWithLowerCase) {
        String[] strings = org.apache.commons.lang.StringUtils.split(value.toLowerCase(), "_");

        for (int i = startWithLowerCase ? 1 : 0; i < strings.length; i++){
            strings[i] = org.apache.commons.lang.StringUtils.capitalize(strings[i]);
        }

        return org.apache.commons.lang.StringUtils.join(strings);
    }

}
