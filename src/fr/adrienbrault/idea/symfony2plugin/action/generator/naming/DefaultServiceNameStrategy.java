package fr.adrienbrault.idea.symfony2plugin.action.generator.naming;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DefaultServiceNameStrategy implements ServiceNameStrategyInterface {

    @Nullable
    @Override
    public String getServiceName(@NotNull ServiceNameStrategyParameter parameter) {

        String className = parameter.getClassName();
        if(className.startsWith("\\")) {
            className = className.substring(1);
        }

        String[] split = className.split("\\\\");

        if (className.contains("Bundle")) {

            int x = -1;
            for (int i = 0; i < split.length; i++) {
                if (split[i].endsWith("Bundle")) {

                    // bundle this our namespace switch:
                    // but dont add on "\Bundle\" only namespace
                    if(split[i].length() > "bundle".length()) {
                        split[i] = split[i].substring(0, split[i].length() - "bundle".length());
                    }

                    x = i + 1;
                }
            }

            Collection<String> parts = new ArrayList<String>();
            parts.add(StringUtils.join(Arrays.copyOfRange(split, 0, x), "_"));

            String[] bundleAfter = Arrays.copyOfRange(split, x, split.length);
            if (bundleAfter.length > 1) {
                parts.add(StringUtils.join(Arrays.copyOfRange(bundleAfter, 0, bundleAfter.length - 1), "_"));
                parts.add(bundleAfter[bundleAfter.length - 1]);
                return formatParts(parts);
            } else if (bundleAfter.length == 1) {
                parts.add(bundleAfter[0]);
                return formatParts(parts);
            }

        }

        return formatParts(Arrays.asList(split));
    }

    private String formatParts(@NotNull Collection<String> parts) {
        Collection<String> partString = parts.stream()
            .map(fr.adrienbrault.idea.symfony2plugin.util.StringUtils::underscore)
            .collect(Collectors.toCollection(ArrayList::new));

        return StringUtils.join(partString, ".");
    }
}
