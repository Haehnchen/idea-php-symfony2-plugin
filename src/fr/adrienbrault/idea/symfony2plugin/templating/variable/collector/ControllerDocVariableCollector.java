package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerDocVariableCollector implements TwigFileVariableCollector, TwigFileVariableCollector.TwigFileVariableCollectorExt {

    public static String DOC_PATTERN  = "\\{#[\\s]+@[C|c]ontroller[\\s]+([\\w\\\\\\[\\]:]+)[\\s]+#}";
    public static String DOC_PATTERN_COMPLETION  = "\\{#[\\s]+@[C|c]ontroller[\\s]+.*#}";

    @Override
    public void collectVars(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables) {

        PsiFile psiFile = parameter.getElement().getContainingFile();
        if(!(psiFile instanceof TwigFile)) {
            return;
        }

        ArrayList<String> controllerNames = findFileControllerDocBlocks((TwigFile) psiFile);
        if(controllerNames.size() == 0) {
            return;
        }

        ControllerIndex controllerIndex = new ControllerIndex(parameter.getProject());

        for(String controllerName: controllerNames) {
            Method method = controllerIndex.resolveShortcutName(controllerName);
            if(method != null) {
                variables.putAll(PhpMethodVariableResolveUtil.collectMethodVariables(method));
            }
        }

    }

    private static ArrayList<String> findFileControllerDocBlocks(TwigFile twigFile) {

        Pattern pattern = Pattern.compile(DOC_PATTERN);

        ArrayList<String> controller = new ArrayList<>();

        for(PsiComment psiComment: PsiTreeUtil.getChildrenOfTypeAsList(twigFile, PsiComment.class)) {
            Matcher matcher = pattern.matcher(psiComment.getText());
            if (matcher.find()) {
                controller.add(matcher.group(1));
            }
        }

        return controller;
    }

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {

    }
}
