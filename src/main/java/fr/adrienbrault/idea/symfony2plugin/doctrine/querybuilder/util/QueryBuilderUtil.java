package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util;

import fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderCompletionContribution;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderCompletionContributionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    public static Collection<QueryBuilderCompletionContribution> guestCompletionContribution(@Nullable String content) {
        Collection<QueryBuilderCompletionContribution> contributions = new ArrayList<>();

        if (content == null || content.isBlank()) {
            contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.PROPERTY, ""));
            contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.FUNCTION, ""));
        }

        if (content != null) {
            if (content.matches("^[\\w+_.]+$")) {
                contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.PROPERTY, content));
            }

            Matcher matcher = Pattern.compile("([\\w_]+)\\.([\\w_]+)$").matcher(content);
            if (matcher.find())  {
                String table = matcher.group(1);
                String field = matcher.group(2);

                contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.PROPERTY, table + "." + field));
            }

            // "foo, test.test"
            // "(test.test"
            Matcher matcher3 = Pattern.compile("[(|,]\\s*([\\w_]+)$").matcher(content);
            if (matcher3.find())  {
                contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.PROPERTY, matcher3.group(1)));
            }

            // "test"
            // ", test"
            Matcher matcher2 = Pattern.compile("[(|,]\\s*([\\w_]+)$").matcher(content);
            if (matcher2.find())  {
                contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.FUNCTION, matcher2.group(1)));
            } else {
                Matcher matcherX = Pattern.compile("^([\\w_]+)$").matcher(content);
                if (matcherX.find())  {
                    contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.FUNCTION, matcherX.group(1)));
                }
            }

            // "(test
            // "( test
            Matcher matcherU = Pattern.compile("\\(\\s*([\\w_]+)$").matcher(content);
            if (matcherU.find())  {
                contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.PROPERTY, matcherU.group(1)));
            }

            // "AND test"
            // "AND test."
            // "> test"
            // "> test."
            Matcher matcherY = Pattern.compile("(AND|OR|WHERE|NOT|=|>|<)\\s+([\\w_]+[.]*)$").matcher(content);
            if (matcherY.find())  {
                contributions.add(new QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType.PROPERTY, matcherY.group(2)));
            }
        }

        return new HashSet<>(contributions);
    }
}
