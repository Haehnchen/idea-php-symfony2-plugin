package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerDocVariableCollector implements TwigFileVariableCollector {

    public static final String DOC_PATTERN  = "\\{#[\\s]+@[C|c]ontroller[\\s]+([\\w\\\\\\[\\]:]+)[\\s]+#}";
    public static final String DOC_PATTERN_COMPLETION  = "\\{#[\\s]+@[C|c]ontroller[\\s]+.*#}";
    private static final Key<CachedValue<List<String>>> CONTROLLER_DOC_BLOCKS_CACHE = new Key<>("TWIG_CONTROLLER_DOC_BLOCKS");

    @Override
    public void collectPsiVariables(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, PsiVariable> variables) {
        PsiFile psiFile = parameter.getContainingFile();
        if(!(psiFile instanceof TwigFile)) {
            return;
        }

        List<String> controllerNames = findFileControllerDocBlocks((TwigFile) psiFile);
        if(controllerNames.isEmpty()) {
            return;
        }

        ControllerIndex controllerIndex = new ControllerIndex(parameter.getProject());

        for(String controllerName: controllerNames) {
            for(Method method : controllerIndex.resolveShortcutName(controllerName)) {
                variables.putAll(PhpMethodVariableResolveUtil.collectMethodVariables(method));
            }
        }
    }

    @NotNull
    private static List<String> findFileControllerDocBlocks(@NotNull TwigFile twigFile) {
        return CachedValuesManager.getCachedValue(
            twigFile,
            CONTROLLER_DOC_BLOCKS_CACHE,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableList(findFileControllerDocBlocksInner(twigFile)),
                twigFile
            )
        );
    }

    @NotNull
    private static List<String> findFileControllerDocBlocksInner(@NotNull TwigFile twigFile) {

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
}
