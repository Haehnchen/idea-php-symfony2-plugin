package fr.adrienbrault.idea.symfony2plugin.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SimilarSuggestionUtil {
    @NotNull
    public static List<String> findSimilarString(@NotNull String string, @NotNull Collection<String> strings) {
        Map<String, Integer> fuzzy = new HashMap<>();

        for (String domain : strings) {
            int fuzzyDistance = org.apache.commons.lang3.StringUtils.getFuzzyDistance(string, domain, Locale.ENGLISH);
            if (fuzzyDistance > 0) {
                fuzzy.put(domain, fuzzyDistance);
            }
        }

        double v = calculateStandardDeviation(Arrays.stream(fuzzy.values().stream().mapToInt(i->i).toArray()).asDoubleStream().toArray());

        Map<String, Integer> fuzzySelected = new HashMap<>();
        for (Map.Entry<String, Integer> entry : fuzzy.entrySet()) {
            if (entry.getValue() > v) {
                fuzzySelected.put(entry.getKey(), entry.getValue());
            }
        }

        return fuzzySelected.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public static double calculateStandardDeviation(double[] numArray) {
        double sum = 0.0;
        double standardDeviation = 0.0;

        int length = numArray.length;

        for(double num : numArray) {
            sum += num;
        }

        double mean = sum / length;

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }
}
