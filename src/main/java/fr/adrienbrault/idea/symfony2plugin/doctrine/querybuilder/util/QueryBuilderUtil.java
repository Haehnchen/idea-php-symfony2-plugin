package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util;

import fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderUtil {

    /**
     * $em->getRepository('Class\Car')->createQueryBuilder('f')->andWhere('<caret>')
     * $em->getRepository(Car::class)->createQueryBuilder('f')->andWhere('<caret>')
     */
    public static Collection<String> extractQueryBuilderRepositoryParameters(@NotNull String content) {
        Collection<String> results = new HashSet<>();

        for(String signature : content.split("\\|")) {
            int endIndex = signature.lastIndexOf(ObjectRepositoryTypeProvider.TRIM_KEY);
            if(endIndex != -1) {
                String parameter = signature.substring(endIndex + 1);
                int point = parameter.indexOf(".createQueryBuilder");
                if(point > -1) {
                    parameter = parameter.substring(0, point);
                    results.add(parameter);
                }
            }
        }

        return results;
    }

    /**
     * test "test.fooTe<caret>aa = aa"
     */
    @Nullable
    public static String getFieldString(@NotNull String content, int offset) {
        if (offset > content.length()) {
            return null;
        }

        String substring1 = content.substring(0, offset);
        Matcher matcherBefore = Pattern.compile("([\\w.]+)[\\s|>=<]?$").matcher(substring1);
        if (!matcherBefore.find()) {
            return null;
        }

        String substring = content.substring(offset);
        Matcher matcherAfter = Pattern.compile("^[\\s|>=<]?([\\w.]+)").matcher(substring);
        if (!matcherAfter.find()) {
            return null;
        }

        String field = matcherBefore.group(1) + matcherAfter.group(1);

        // invalid field
        if (!field.contains(".")) {
            return null;
        }

        return field;
    }
}
