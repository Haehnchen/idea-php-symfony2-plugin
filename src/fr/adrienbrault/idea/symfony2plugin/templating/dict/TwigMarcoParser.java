package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.psi.PsiFile;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigMarcoParser {

    public HashMap<String, String> getMacros(PsiFile file) {

        HashMap<String, String> current = new HashMap<String, String>();

        Matcher matcherBlocks = Pattern.compile("\\{%[\\s+]*macro[\\s+]*(.*?)[\\s+]*\\((.*?)\\)").matcher(file.getText());
        while(matcherBlocks.find()){
            current.put(matcherBlocks.group(1), matcherBlocks.group(2));
        }

        return current;
    }

}
