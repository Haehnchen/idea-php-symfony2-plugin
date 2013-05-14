package fr.adrienbrault.idea.symfony2plugin.util.annotation;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

public class AnnotationLookupElement extends LookupElement {

    private String name;
    private InsertHandler<LookupElement> insertHandler = null;
    private AnnotationValue annotationValue = null;

    public AnnotationLookupElement(String name, AnnotationValue annotationValue, InsertHandler<LookupElement> insertHandler) {
        this.name = name;
        this.insertHandler = insertHandler;
        this.annotationValue = annotationValue;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return name;
    }

    @NotNull
    public AnnotationValue getObject() {
        return this.annotationValue;
    }

    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
        }
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(Symfony2Icons.SYMFONY);
        presentation.setTypeText(this.annotationValue.getType() == AnnotationValue.Type.Array ? "Array" : "Value");
        presentation.setTypeGrayed(true);
    }

}

