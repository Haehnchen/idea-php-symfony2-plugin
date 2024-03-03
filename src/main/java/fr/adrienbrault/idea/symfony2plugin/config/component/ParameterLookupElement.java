package fr.adrienbrault.idea.symfony2plugin.config.component;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterLookupElement extends LookupElement {

    protected Object psiElement;
    protected InsertHandler<LookupElement> insertHandler = null;
    protected final ContainerParameter containerParameter;

    public ParameterLookupElement(@NotNull ContainerParameter containerParameter) {
        this.containerParameter = containerParameter;
    }

    public ParameterLookupElement(@NotNull ContainerParameter containerParameter, @Nullable InsertHandler<LookupElement> insertHandler, @Nullable Object psiElement) {
        this.insertHandler = insertHandler;
        this.psiElement = psiElement;
        this.containerParameter = containerParameter;
    }

    @NotNull
    @Override
    public Object getObject() {
        return this.psiElement != null ? this.psiElement : this;
    }

    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
        }
    }

    @NotNull
    @Override
    public String getLookupString() {
        return containerParameter.getName();
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(containerParameter.getName());

        String value = containerParameter.getValue();
        if(value != null && StringUtils.isNotBlank(value)) {
            presentation.setTypeText(value);
        }

        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.PARAMETER);

        if(this.containerParameter.isWeak()) {
            presentation.setIcon(Symfony2Icons.PARAMETER_OPACITY);
        }

    }

}