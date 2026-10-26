package fr.adrienbrault.idea.symfony2plugin.form.completion;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.completion.lookup.ClassConstantLookupElementAbstract;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormClassConstantsLookupElement extends ClassConstantLookupElementAbstract {
    private final FormTypeClass.Type type;

    public FormClassConstantsLookupElement(@NotNull PhpClass phpClass, @NotNull FormTypeClass.Type type) {
        super(phpClass);
        this.type = type;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        if (type == FormTypeClass.Type.PLAIN) {
            attachPlainTypeTail(presentation);
        }

        presentation.setIcon(Symfony2Icons.FORM_TYPE);
        super.renderElement(presentation);
    }

    private void attachPlainTypeTail(@NotNull LookupElementPresentation presentation) {
        // provides parent and string alias name
        List<String> tailsText = new ArrayList<>();

        String getName = PhpElementsUtil.getMethodReturnAsString(phpClass, "getName");
        if(getName != null) {
            tailsText.add(getName);
        }

        // @TODO: getParent should
        String getParent = PhpElementsUtil.getMethodReturnAsString(phpClass, "getParent");
        if(getParent != null && !getParent.equals("form")) {
            tailsText.add(getParent);
        }

        if(!tailsText.isEmpty()) {
            presentation.setTailText("(" + StringUtils.join(tailsText, ",") + ")", true);
        }
    }
}
