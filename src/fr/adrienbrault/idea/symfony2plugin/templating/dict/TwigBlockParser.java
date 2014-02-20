package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigBlockParser {

    public static String EXTENDS_TEMPLATE_NAME_PATTERN = "^[\\s+]*\\{%\\s+extends[\\s+]*['|\"](.*?)['|\"]";

    private Map<String, TwigFile> twigFilesByName;

    public TwigBlockParser(Map<String, TwigFile> twigFilesByName) {
        this.twigFilesByName = twigFilesByName;
    }

    public ArrayList<TwigBlock> walk(@Nullable PsiFile file) {
        return this.walk(file, "self", new ArrayList<TwigBlock>(), 0);
    }

    public ArrayList<TwigBlock> walk(@Nullable PsiFile file, String shortcutName, ArrayList<TwigBlock> current, int depth) {

        if(file == null) {
            return current;
        }

        // @TODO: we dont use psielements here, check if Pattern faster or not

        // dont match on self file !?
        if(depth > 0) {
            Matcher matcherBlocks = Pattern.compile("\\{%[\\s+]*block[\\s+]*(.*?)[\\s+]*%}").matcher(file.getText());
            while(matcherBlocks.find()){
                current.add(new TwigBlock(matcherBlocks.group(1), shortcutName, file));
            }
        }

        // limit recursive calls
        if(depth++ > 20) {
            return current;
        }

        // find extend in self
        Matcher matcher = Pattern.compile(EXTENDS_TEMPLATE_NAME_PATTERN).matcher(file.getText());
        while(matcher.find()){
            if(twigFilesByName.containsKey(TwigHelper.normalizeTemplateName(matcher.group(1)))) {
                TwigFile twigFile = twigFilesByName.get(matcher.group(1));
                this.walk(twigFile, matcher.group(1), current, depth);
            }
        }

        return current;
    }

}
