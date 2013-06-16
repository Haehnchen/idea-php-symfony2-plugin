package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class RouteLookupElement  extends LookupElement {

    private Route route;

    public RouteLookupElement(Route route) {
        this.route = route;
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
        presentation.setIcon(Symfony2Icons.ROUTE);
    }

}
