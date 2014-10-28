package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteLookupElement;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TwigPathFunctionInsertHandler implements InsertHandler<RouteLookupElement> {

    private static final TwigPathFunctionInsertHandler instance = new TwigPathFunctionInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull RouteLookupElement lookupElement) {
        context.getDocument().insertString(context.getStartOffset(), "{{ path('");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'");

        Set<String> vars = lookupElement.getRoute().getVariables();
        if(vars.size() > 0) {
            List<String> items = new ArrayList<String>();
            for(String var: vars) {
                items.add(String.format("'%s': '%s'", var, "x"));
            }
            stringBuilder.append(", {");
            stringBuilder.append(StringUtils.join(items, ", "));
            stringBuilder.append("}");
        }

        stringBuilder.append(") }}");
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), stringBuilder.toString());
    }

    public static TwigPathFunctionInsertHandler getInstance(){
        return instance;
    }

}