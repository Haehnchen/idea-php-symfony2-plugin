package fr.adrienbrault.idea.symfony2plugin.config.component;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterLookupElement extends LookupElement {

    private Object psiElement;
    private InsertHandler<LookupElement> insertHandler = null;
    private ContainerParameter containerParameter;

    public ParameterLookupElement(ContainerParameter containerParameter) {
        this.containerParameter = containerParameter;
    }

    public ParameterLookupElement(ContainerParameter containerParameter, InsertHandler<LookupElement> insertHandler, Object psiElement) {
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
        presentation.setItemText(getLookupString());

        String value = containerParameter.getValue();
        if(value != null && StringUtils.isNotBlank(value)) {
            presentation.setTypeText(value);
        }

        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.PARAMETER);

        if(this.containerParameter != null && this.containerParameter.isWeak()) {
            presentation.setIcon(Symfony2Icons.PARAMETER_OPACITY);
        }

    }

}