package fr.adrienbrault.idea.symfony2plugin.util;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StringUtils {

    public static String camelize(String input) {
        return camelize(input, false);
    }

    public static String camelize(String input, boolean startWithLowerCase) {

        String[] strings = org.apache.commons.lang3.StringUtils.split(input.toLowerCase(), "_");
        for (int i = startWithLowerCase ? 1 : 0; i < strings.length; i++){
            strings[i] = org.apache.commons.lang3.StringUtils.capitalize(strings[i]);
        }

        input = org.apache.commons.lang3.StringUtils.join(strings);

        if(!startWithLowerCase) {
            return ucfirst(input);
        }

        return input;
    }

    public static String underscore(String camelCasedWord) {
        return org.apache.commons.lang3.StringUtils.capitalize(camelCasedWord).replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    public static String ucfirst(String chaine){
        if(chaine.isEmpty()) {
            return chaine;
        }
        return chaine.substring(0, 1).toUpperCase() + chaine.substring(1);
    }

    @NotNull
    public static String lcfirst(@NotNull String input){
        if(input.isEmpty()) {
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

    @NotNull
    public static String removeEnd(@NotNull String str, @NotNull String suffix) {
        return str.endsWith(suffix) ? str.substring(0, str.length() - suffix.length()) : str;
    }

    @NotNull
    public static String removeEndIgnoreCase(@NotNull String str, @NotNull String suffix) {
        if (suffix.isEmpty()) return str;
        if (str.length() >= suffix.length() && str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length())) {
            return str.substring(0, str.length() - suffix.length());
        }
        return str;
    }

    @NotNull
    public static String removeStartIgnoreCase(@NotNull String str, @NotNull String prefix) {
        if (prefix.isEmpty()) return str;
        if (str.length() >= prefix.length() && str.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return str.substring(prefix.length());
        }
        return str;
    }

    /**
     * Replacement for deprecated Apache Commons StringUtils.getFuzzyDistance.
     * Awards +1 per character match, +3 for consecutive matches (case-insensitive).
     */
    public static int getFuzzyDistance(@NotNull CharSequence term, @NotNull CharSequence query) {
        String termLower = term.toString().toLowerCase(Locale.ENGLISH);
        String queryLower = query.toString().toLowerCase(Locale.ENGLISH);

        int score = 0;
        int termIndex = 0;
        int lastMatchIndex = Integer.MIN_VALUE;

        for (int queryIndex = 0; queryIndex < queryLower.length(); queryIndex++) {
            char queryChar = queryLower.charAt(queryIndex);
            boolean found = false;

            while (termIndex < termLower.length() && !found) {
                char termChar = termLower.charAt(termIndex);
                if (queryChar == termChar) {
                    score++;
                    if (lastMatchIndex + 1 == termIndex) {
                        score += 2;
                    }
                    lastMatchIndex = termIndex;
                    found = true;
                }
                termIndex++;
            }
        }

        return score;
    }
}
