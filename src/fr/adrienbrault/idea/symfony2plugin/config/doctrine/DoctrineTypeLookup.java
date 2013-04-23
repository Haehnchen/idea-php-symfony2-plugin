package fr.adrienbrault.idea.symfony2plugin.config.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;

import com.intellij.openapi.vfs.VfsUtil;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

public class DoctrineTypeLookup extends LookupElement {

    private String name;

    public DoctrineTypeLookup(String typeName) {
        this.name = typeName;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return name;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(Symfony2Icons.DOCTRINE);
        presentation.setTypeText("Doctrine");
        presentation.setTypeGrayed(true);
    }

}