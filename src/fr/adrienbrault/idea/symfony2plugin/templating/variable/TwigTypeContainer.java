package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TwigTypeContainer {

    private PhpNamedElement phpNamedElement;
    private String stringElement;

    public TwigTypeContainer(PhpNamedElement phpNamedElement) {
        this.phpNamedElement = phpNamedElement;
    }

    public TwigTypeContainer(String stringElement) {
        this.stringElement = stringElement;
    }

    @Nullable
    public PhpNamedElement getPhpNamedElement() {
        return phpNamedElement;
    }

    @Nullable
    public String getStringElement() {
        return stringElement;
    }

    public static Collection<TwigTypeContainer> fromCollection(Collection<? extends PhpNamedElement> phpNamedElements) {

        List<TwigTypeContainer> twigTypeContainerList = new ArrayList<TwigTypeContainer>();

        for(PhpNamedElement phpNamedElement :phpNamedElements) {
            twigTypeContainerList.add(new TwigTypeContainer(phpNamedElement));
        }

        return twigTypeContainerList;
    }


}