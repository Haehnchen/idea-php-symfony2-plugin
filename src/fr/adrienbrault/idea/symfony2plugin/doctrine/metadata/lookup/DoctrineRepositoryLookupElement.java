package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.lookup;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineRepositoryLookupElement extends LookupElement {

    @NotNull
    private final PhpClass phpClass;
    private InsertHandler<DoctrineRepositoryLookupElement> insertHandler;

    private DoctrineRepositoryLookupElement(@NotNull PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(phpClass.getName());
        presentation.setTypeText(phpClass.getPresentableFQN());
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.DOCTRINE);
    }

    @NotNull
    @Override
    public String getLookupString() {
        return StringUtils.stripStart(phpClass.getPresentableFQN(), "\\");
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
