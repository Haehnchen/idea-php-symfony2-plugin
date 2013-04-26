package fr.adrienbrault.idea.symfony2plugin.config.component;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterLookupElement extends LookupElement {

    private String parameterKey;
    private String parameterValue;

    public ParameterLookupElement(String parameterKey, String parameterValue) {
        this.parameterKey = parameterKey;
        this.parameterValue = parameterValue;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return parameterKey;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(parameterValue);
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.SYMFONY);
    }

}