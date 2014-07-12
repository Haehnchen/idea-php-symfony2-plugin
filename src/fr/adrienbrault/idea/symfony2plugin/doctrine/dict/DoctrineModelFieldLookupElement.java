package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.PhpIcons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModelFieldLookupElement extends LookupElement {

    final private DoctrineModelField doctrineModelField;
    private boolean withBoldness = false;
    private String withLookupName = null;

    public DoctrineModelFieldLookupElement(@NotNull DoctrineModelField doctrineModelField) {
        this.doctrineModelField = doctrineModelField;
    }

    @NotNull
    @Override
    public String getLookupString() {
        if(this.withLookupName != null) {
            return this.withLookupName;
        }

        return this.doctrineModelField.getName();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        super.renderElement(presentation);

        presentation.setItemTextBold(withBoldness);
        presentation.setIcon(Symfony2Icons.DOCTRINE);
        presentation.setTypeGrayed(true);

        if(this.doctrineModelField.getTypeName() != null) {
            presentation.setTypeText(this.doctrineModelField.getTypeName());
        }

        if(this.doctrineModelField.getRelationType() != null) {
            presentation.setTailText(String.format("(%s)", this.doctrineModelField.getRelationType()), true);
        }

        if(this.doctrineModelField.getRelation() != null) {
            presentation.setTypeText(this.doctrineModelField.getRelation());
            presentation.setIcon(PhpIcons.CLASS_ICON);
        }

    }


    public DoctrineModelFieldLookupElement withBoldness(boolean withBoldness) {
        this.withBoldness = withBoldness;
        return this;
    }

    public DoctrineModelFieldLookupElement withLookupName(String withLookupName) {
        this.withLookupName = withLookupName;
        return this;
    }

}

