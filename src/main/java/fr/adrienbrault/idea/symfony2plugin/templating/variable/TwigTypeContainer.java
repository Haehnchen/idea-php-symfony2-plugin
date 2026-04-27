package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormViewDataHolder;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTypeContainer {

    private final PhpNamedElement phpNamedElement;
    private final String stringElement;
    private final FormFieldDataHolder formFieldDataHolder;
    private final FormViewDataHolder formViewDataHolder;

    public TwigTypeContainer(PhpNamedElement phpNamedElement) {
        this(phpNamedElement, null);
    }

    public TwigTypeContainer(PhpNamedElement phpNamedElement, @Nullable FormViewDataHolder formViewDataHolder) {
        this.phpNamedElement = phpNamedElement;
        this.stringElement = null;
        this.formFieldDataHolder = null;
        this.formViewDataHolder = formViewDataHolder;
    }

    public TwigTypeContainer(String stringElement) {
        this(stringElement, null);
    }

    public TwigTypeContainer(String stringElement, @Nullable FormFieldDataHolder formFieldDataHolder) {
        this.phpNamedElement = null;
        this.stringElement = stringElement;
        this.formFieldDataHolder = formFieldDataHolder;
        this.formViewDataHolder = null;
    }

    @Nullable
    public PhpNamedElement getPhpNamedElement() {
        return phpNamedElement;
    }

    @Nullable
    public String getStringElement() {
        return stringElement;
    }

    public static Collection<TwigTypeContainer> fromCollection(Project project, Collection<PsiVariable> psiVariables) {

        List<TwigTypeContainer> twigTypeContainerList = new ArrayList<>();

        for(PsiVariable phpNamedElement :psiVariables) {
            Collection<PhpClass> phpClass = PhpElementsUtil.getClassFromPhpTypeSet(project, phpNamedElement.getTypes());
            if(!phpClass.isEmpty()) {
                FormViewDataHolder formViewDataHolder = phpNamedElement.getFormTypeFqns().isEmpty()
                    ? null
                    : new FormViewDataHolder(phpNamedElement.getFormTypeFqns());

                twigTypeContainerList.add(new TwigTypeContainer(phpClass.iterator().next(), formViewDataHolder));
            }
        }

        return twigTypeContainerList;
    }

    @Nullable
    public FormFieldDataHolder getFormFieldDataHolder() {
        return formFieldDataHolder;
    }

    @Nullable
    public FormViewDataHolder getFormViewDataHolder() {
        return formViewDataHolder;
    }

}
