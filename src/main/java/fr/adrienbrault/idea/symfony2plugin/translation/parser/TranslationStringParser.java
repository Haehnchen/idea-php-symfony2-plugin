package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationStringParser {

    public TranslationStringMap parse(String file) {
        return this.parse(new File(file));
    }

    public TranslationStringMap parsePathMatcher(String path) {

        File file = new File(path);
        File[] files = file.listFiles();

        if(null == files) {
            return new TranslationStringMap();
        }

        for (final File fileEntry : files) {
            if (!fileEntry.isDirectory()) {
                String fileName = fileEntry.getName();
                if( null != fileName && fileName.endsWith("php")) {
                    return this.parse(fileEntry);
                }
            }
        }

        return new TranslationStringMap();
    }

    @Nullable
    public TranslationStringMap parse(File file) {

        String string = "";
        try {
            string = this.fileToString(file.getPath());
        } catch (IOException e) {
            return null;
        }

        TranslationStringMap string_map = new TranslationStringMap();

        Matcher matcher = Pattern.compile("'([\\w]+)'\\s=>.*?array\\s\\((.*?)\\s\\),", Pattern.MULTILINE).matcher(string);
        while(matcher.find()){
            String domain = matcher.group(1);
            String array_strings = matcher.group(2);

            Matcher match_strings = Pattern.compile("\\s'(.*?)'\\s=>\\s'.*?',\\s", Pattern.MULTILINE).matcher(array_strings);
            while(match_strings.find()){
                string_map.addString(domain, match_strings.group(1));
            }

        }

        return string_map;
    }

    private String fileToString(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder builder = new StringBuilder();
        String line;

        while((line = reader.readLine()) != null){
            builder.append(line);
        }

        return builder.toString();
    }

}
