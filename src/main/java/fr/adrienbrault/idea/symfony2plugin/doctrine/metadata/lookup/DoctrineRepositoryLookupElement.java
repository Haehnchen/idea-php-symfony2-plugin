package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.lookup;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineRepositoryLookupElement extends LookupElement {
    private final @NotNull String phpClassPresentableFQN;

    private final @NotNull String phpClassName;

    private DoctrineRepositoryLookupElement(@NotNull PhpClass phpClass) {
        this.phpClassName = phpClass.getName();
        this.phpClassPresentableFQN = phpClass.getPresentableFQN();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(phpClassName);
        presentation.setTypeText(phpClassPresentableFQN);
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.DOCTRINE);
    }

    @NotNull
    @Override
    public String getLookupString() {
        return StringUtils.stripStart(phpClassPresentableFQN, "\\");
    }

    public static DoctrineRepositoryLookupElement create(@NotNull PhpClass phpClass) {
        return new DoctrineRepositoryLookupElement(phpClass);
    }

    public static Collection<DoctrineRepositoryLookupElement> create(@NotNull Collection<PhpClass> phpClasses) {
        Collection<DoctrineRepositoryLookupElement> elements = new ArrayList<>();
        for(PhpClass phpClass: phpClasses) {
            elements.add(DoctrineRepositoryLookupElement.create(phpClass));
        }

        return elements;
    }
}
