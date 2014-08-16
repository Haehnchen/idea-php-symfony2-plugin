package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigBlockParser {

    public static String EXTENDS_TEMPLATE_NAME_PATTERN = "^[\\s+]*\\{%\\s+extends[\\s+]*['|\"](.*?)['|\"]";

    private Map<String, VirtualFile> twigFilesByName;

    public TwigBlockParser(Map<String, VirtualFile> twigFilesByName) {
        this.twigFilesByName = twigFilesByName;
    }

    public List<TwigBlock> walk(@Nullable PsiFile file) {
        return this.walk(file, "self", new ArrayList<TwigBlock>(), 0);
    }

    public List<TwigBlock> walk(@Nullable PsiFile file, String shortcutName, List<TwigBlock> current, int depth) {

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
            String templateName = TwigHelper.normalizeTemplateName(matcher.group(1));
            if(twigFilesByName.containsKey(templateName)) {
                VirtualFile virtualFile = twigFilesByName.get(templateName);
                PsiFile psiFile = PsiManager.getInstance(file.getProject()).findFile(virtualFile);
                if(psiFile instanceof TwigFile) {
                    this.walk(psiFile, matcher.group(1), current, depth);
                }
            }
        }

        return current;
    }

}
