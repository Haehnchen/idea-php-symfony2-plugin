package fr.adrienbrault.idea.symfonyplugin.doctrine.querybuilder.util;

import fr.adrienbrault.idea.symfonyplugin.doctrine.ObjectRepositoryTypeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

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
}
