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

    private String parameterKey;
    private String parameterValue;
    private Object psiElement;
    private InsertHandler<LookupElement> insertHandler = null;
    private ContainerParameter containerParameter;

    public ParameterLookupElement(ContainerParameter containerParameter, InsertHandler<LookupElement> insertHandler, Object psiElement) {
        this(containerParameter.getName(), containerParameter.getValue(), insertHandler, psiElement);
        this.containerParameter = containerParameter;
    }

    @Deprecated
    public ParameterLookupElement(String parameterKey, String parameterValue) {
        this.parameterKey = parameterKey;
        this.parameterValue = parameterValue;
    }

    @Deprecated
    public ParameterLookupElement(String name, String value, InsertHandler<LookupElement> insertHandler, Object psiElement) {
        this(name, value);
        this.insertHandler = insertHandler;
        this.psiElement = psiElement;
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
        return parameterKey;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());

        if(parameterValue != null && !StringUtils.isBlank(parameterValue)) {
            presentation.setTypeText(parameterValue);
        }

        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.PARAMETER);

        if(this.containerParameter != null && this.containerParameter.isWeak()) {
            presentation.setIcon(Symfony2Icons.PARAMETER_OPACITY);
        }

    }

}