package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModelFieldLookupElement extends LookupElement {

    final private DoctrineModelField modelField;

    public DoctrineModelFieldLookupElement(DoctrineModelField modelField) {
        this.modelField = modelField;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return modelField.getName();
    }

    public void renderElement(LookupElementPresentation presentation) {

        presentation.setIcon(Symfony2Icons.DOCTRINE);
        presentation.setItemText(modelField.getName());

        if(modelField.getTypeName() != null) {
            presentation.setTypeText(modelField.getTypeName());
        }

    }

}

