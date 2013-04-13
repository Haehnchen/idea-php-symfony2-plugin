package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineEntityLookupElement extends LookupElement {

    private String entityName;
    private PhpClass className;

    public DoctrineEntityLookupElement(String entityName, PhpClass className) {
        this.entityName = entityName;
        this.className = className;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return entityName;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(className.getPresentableFQN());
        presentation.setTypeGrayed(true);
        presentation.setIcon(PhpIcons.CLASS);
    }

}

