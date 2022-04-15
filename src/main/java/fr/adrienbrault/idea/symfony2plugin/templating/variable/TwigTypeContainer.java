package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTypeContainer {
    @Nullable
    private PhpNamedElement phpNamedElement;

    @Nullable
    private String stringElement;

    @Nullable
    private Object dataHolder;

    public TwigTypeContainer(@Nullable PhpNamedElement phpNamedElement) {
        this.phpNamedElement = phpNamedElement;
    }

    public TwigTypeContainer(@Nullable String stringElement) {
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

    public static Collection<TwigTypeContainer> fromCollection(Project project, Collection<PsiVariable> psiVariables) {

        List<TwigTypeContainer> twigTypeContainerList = new ArrayList<>();

        for(PsiVariable phpNamedElement :psiVariables) {
            Collection<PhpClass> phpClass = PhpElementsUtil.getClassFromPhpTypeSet(project, phpNamedElement.getTypes());
            if(phpClass.size() > 0) {
                twigTypeContainerList.add(new TwigTypeContainer(phpClass.iterator().next()));
            }
        }

        return twigTypeContainerList;
    }

    public TwigTypeContainer withDataHolder(@NotNull Object object) {
        this.dataHolder = object;
        return this;
    }

    @Nullable
    public Object getDataHolder() {
        return dataHolder;
    }
}