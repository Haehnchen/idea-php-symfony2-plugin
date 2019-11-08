package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.PhpIcons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerActionLookupElement extends LookupElement {

    private ControllerAction controllerAction;

    public ControllerActionLookupElement(ControllerAction controllerAction) {
        this.controllerAction = controllerAction;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return this.controllerAction.getShortcutName();
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(StringUtils.stripStart(controllerAction.getMethod().getFQN(), "\\"));
        presentation.setTypeGrayed(true);
        presentation.setIcon(PhpIcons.METHOD_ICON);
    }

}