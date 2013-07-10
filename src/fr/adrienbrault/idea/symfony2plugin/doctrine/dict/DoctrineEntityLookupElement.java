package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineEntityLookupElement extends LookupElement {

    private String entityName;
    private PhpClass className;
    private boolean useClassNameAsLookupString = false;

    public DoctrineEntityLookupElement(String entityName, PhpClass className, boolean useClassNameAsLookupString) {
        this(entityName, className);
        this.useClassNameAsLookupString = useClassNameAsLookupString;
    }

    public DoctrineEntityLookupElement(String entityName, PhpClass className) {
        this.entityName = entityName;
        this.className = className;
    }

    @NotNull
    @Override
    public String getLookupString() {

        if(this.useClassNameAsLookupString) {
            String className = this.className.getPresentableFQN();
            return className == null ? "" : className;
        }

        return entityName;
    }

    public void renderElement(LookupElementPresentation presentation) {

        if(this.useClassNameAsLookupString) {
            presentation.setItemText(className.getPresentableFQN());
            presentation.setTypeText(getLookupString());
        } else {
            presentation.setItemText(getLookupString());
            presentation.setTypeText(className.getPresentableFQN());
        }

        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.DOCTRINE);
    }

}

