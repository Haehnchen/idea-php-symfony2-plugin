package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetEnum;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ResourceFileInsertHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteLookupElement extends LookupElement {

    final private Route route;
    private boolean isWeak = false;
    private InsertHandler<RouteLookupElement> insertHandler;

    public RouteLookupElement(Route route) {
        this.route = route;
    }

    public RouteLookupElement(Route route, boolean isWeak) {
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
