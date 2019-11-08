package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineEntityLookupElement extends LookupElement {

    private String entityName;
    private PhpClass className;
    private boolean useClassNameAsLookupString = false;
    private DoctrineTypes.Manager manager;
    private boolean isWeak = false;

    public DoctrineEntityLookupElement(String entityName, PhpClass className, boolean useClassNameAsLookupString) {
        this(entityName, className);
        this.useClassNameAsLookupString = useClassNameAsLookupString;
    }

    public DoctrineEntityLookupElement(String entityName, PhpClass className, boolean useClassNameAsLookupString, boolean isWeak) {
        this(entityName, className, useClassNameAsLookupString);
        this.isWeak = isWeak;
    }

    public DoctrineEntityLookupElement(String entityName, PhpClass className) {
        this.entityName = entityName;
        this.className = className;
    }

    public DoctrineEntityLookupElement withManager(DoctrineTypes.Manager manager) {
        this.manager = manager;
        return this;
    }

    @NotNull
    @Override
    public String getLookupString() {

        if(this.useClassNameAsLookupString) {
            return StringUtils.stripStart(this.className.getPresentableFQN(), "\\");
        }

        return entityName;
    }

    public void renderElement(LookupElementPresentation presentation) {

        if(this.useClassNameAsLookupString) {
            presentation.setItemText(className.getPresentableFQN());
            presentation.setTypeText(getLookupString());
        } else {
            presentation.setItemText(getLookupString());
            presentation.setTypeText(className.getPresentableFQN());
        }

        presentation.setTypeGrayed(true);
        if(isWeak) {
            // @TODO: remove weak
            presentation.setIcon(getWeakIcon());
        } else {
            presentation.setIcon(getIcon());
        }


    }

    @Nullable
    private Icon getWeakIcon() {
        if(manager == null) {
            return null;
        }

        if(manager == DoctrineTypes.Manager.ORM) {
            return Symfony2Icons.DOCTRINE_WEAK;
        }

        if(manager == DoctrineTypes.Manager.MONGO_DB || manager == DoctrineTypes.Manager.COUCH_DB) {
            return Symfony2Icons.NO_SQL;
        }

        return null;
    }

    @Nullable
    private Icon getIcon() {
        if(manager == null) {
            return null;
        }

        if(manager == DoctrineTypes.Manager.ORM) {
            return Symfony2Icons.DOCTRINE;
        }

        if(manager == DoctrineTypes.Manager.MONGO_DB || manager == DoctrineTypes.Manager.COUCH_DB) {
            return Symfony2Icons.NO_SQL;
        }

        return null;
    }

}

