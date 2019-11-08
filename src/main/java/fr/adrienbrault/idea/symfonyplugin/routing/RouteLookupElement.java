package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteLookupElement extends LookupElement {

    final private Route route;
    private boolean isWeak = false;
    private InsertHandler<RouteLookupElement> insertHandler;

    public RouteLookupElement(@NotNull Route route) {
        this.route = route;
    }

    public RouteLookupElement(@NotNull Route route, boolean isWeak) {
        this(route);
        this.isWeak = isWeak;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return route.getName();
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(route.getController());
        presentation.setTypeGrayed(true);
        presentation.setIcon(!this.isWeak ? Symfony2Icons.ROUTE : Symfony2Icons.ROUTE_WEAK);

        formatTail(presentation);
    }

    /**
     * "(GET|POST, var1, var2)"
     */
    private void formatTail(@NotNull LookupElementPresentation presentation) {
        List<String> tails = new ArrayList<>();

        Collection<String> methods = route.getMethods();
        if(methods.size() > 0) {
            tails.add(StringUtils.join(ContainerUtil.map(methods, String::toUpperCase), "|"));
        }

        Set<String> variables = this.route.getVariables();
        if(variables.size() > 0) {
            tails.addAll(variables);
        }

        if(tails.size() > 0) {
            presentation.setTailText("(" + StringUtils.join(tails, ", ") + ")", true);
        }
    }

    @Override
    public void handleInsert(InsertionContext context) {
        if(insertHandler != null) {
            insertHandler.handleInsert(context, this);
        }
        super.handleInsert(context);
    }

    public void withInsertHandler(InsertHandler<RouteLookupElement> insertHandler) {
        this.insertHandler = insertHandler;
    }

    public Route getRoute() {
        return route;
    }

}
