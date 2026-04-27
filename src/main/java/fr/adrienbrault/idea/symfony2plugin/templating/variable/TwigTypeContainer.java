package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormViewDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTypeContainer {

    private final Set<String> types = new HashSet<>();
    private final String stringElement;
    private final FormFieldDataHolder formFieldDataHolder;
    private final FormViewDataHolder formViewDataHolder;

    public TwigTypeContainer(@NotNull Collection<String> types) {
        this(types, null);
    }

    private TwigTypeContainer(@NotNull Collection<String> types, @Nullable FormViewDataHolder formViewDataHolder) {
        this.types.addAll(types);
        this.stringElement = null;
        this.formFieldDataHolder = null;
        this.formViewDataHolder = formViewDataHolder;
    }

    public TwigTypeContainer(String stringElement) {
        this(stringElement, null);
    }

    public TwigTypeContainer(String stringElement, @Nullable FormFieldDataHolder formFieldDataHolder) {
        this.stringElement = stringElement;
        this.formFieldDataHolder = formFieldDataHolder;
        this.formViewDataHolder = null;
    }

    @NotNull
    public Set<String> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    @Nullable
    public String getStringElement() {
        return stringElement;
    }

    public static Collection<TwigTypeContainer> fromCollection(Collection<PsiVariable> psiVariables) {

        List<TwigTypeContainer> twigTypeContainerList = new ArrayList<>();

        for(PsiVariable psiVariable : psiVariables) {
            if (psiVariable.getTypes().isEmpty()) {
                continue;
            }

            FormViewDataHolder formViewDataHolder = psiVariable.getFormTypeFqns().isEmpty()
                ? null
                : new FormViewDataHolder(psiVariable.getFormTypeFqns());

            twigTypeContainerList.add(new TwigTypeContainer(psiVariable.getTypes(), formViewDataHolder));
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
