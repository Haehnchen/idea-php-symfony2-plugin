package fr.adrienbrault.idea.symfony2plugin.stubs.util;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherUtil {

    @Nullable
    public static String extractEventClassInstance(@Nullable String contents) {
        if(contents == null || StringUtils.isBlank(contents)) {
            return null;
        }

        contents = contents.replace(" * ", "");

        Matcher matcher = Pattern.compile("method\\s*receive\\w*\\s*[\\w]{1,3}\\s*([\\w\\\\]+)\\s*(instance)*").matcher(contents);
        if (!matcher.find()) {
            return null;
        }

        return StringUtils.stripStart(matcher.group(1).trim(), "\\");
    }

}
