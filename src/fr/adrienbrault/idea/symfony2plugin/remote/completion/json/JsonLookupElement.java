package fr.adrienbrault.idea.symfony2plugin.remote.completion.json;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.Field;

public class JsonLookupElement extends LookupElement {

    private final JsonRawLookupElement jsonLookupElement;

    public JsonLookupElement(JsonRawLookupElement jsonLookupElement) {
        this.jsonLookupElement = jsonLookupElement;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return jsonLookupElement.getLookupString();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {

        if(jsonLookupElement.getTailText() != null) {
            presentation.setTailText(jsonLookupElement.getTailText(), true);
        }

        if(jsonLookupElement.getTypeText() != null) {
            presentation.setTypeText(jsonLookupElement.getTypeText());
            presentation.setTypeGrayed(true);
        }

        decorateIcon(presentation);

        presentation.setItemText(jsonLookupElement.getLookupString());

    }

    private void decorateIcon(LookupElementPresentation presentation) {
        String icon = jsonLookupElement.getIcon();
        if(icon == null) {
            return;
        }

        int endIndex = icon.lastIndexOf(".");
        if(endIndex < 0 || icon.length() - endIndex < 1) {
            return;
        }

        String className = icon.substring(0, endIndex);

        try {
            Class<?> iconClass = Class.forName(className);
            Field field = iconClass.getDeclaredField(icon.substring(endIndex + 1));
            presentation.setIcon((Icon) field.get(null));

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

}




