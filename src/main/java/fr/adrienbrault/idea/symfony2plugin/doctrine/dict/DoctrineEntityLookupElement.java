package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineEntityLookupElement extends LookupElement {
    private final String entityName;
    private final String className;
    private boolean useClassNameAsLookupString = false;
    private DoctrineTypes.Manager manager;
    private boolean isWeak = false;

    public DoctrineEntityLookupElement(String entityName, PhpClass className, boolean useClassNameAsLookupString) {
        this(entityName, className);
        this.useClassNameAsLookupString = useClassNameAsLookupString;
    }

    public DoctrineEntityLookupElement(String entityName, PhpClass phpClass, boolean useClassNameAsLookupString, boolean isWeak) {
        this(entityName, phpClass, useClassNameAsLookupString);
        this.isWeak = isWeak;
    }

    public DoctrineEntityLookupElement(String entityName, PhpClass phpClass) {
        this.entityName = entityName;
        this.className = StringUtils.stripStart(phpClass.getFQN(), "\\");
    }

    public DoctrineEntityLookupElement withManager(DoctrineTypes.Manager manager) {
        this.manager = manager;
        return this;
    }

    @NotNull
    @Override
    public String getLookupString() {
        if (this.useClassNameAsLookupString) {
            return this.className;
        }

        return entityName;
    }

    public void renderElement(@NotNull LookupElementPresentation presentation) {
        if (this.useClassNameAsLookupString) {
            presentation.setItemText(className);
            presentation.setTypeText(getLookupString());
        } else {
            presentation.setItemText(getLookupString());
            presentation.setTypeText(className);
        }

        presentation.setTypeGrayed(true);
        if (isWeak) {
            // @TODO: remove weak
            presentation.setIcon(getWeakIcon());
        } else {
            presentation.setIcon(getIcon());
        }
    }

    @Nullable
    private Icon getWeakIcon() {
        if (manager == null) {
            return null;
        }

        if (manager == DoctrineTypes.Manager.ORM) {
            return Symfony2Icons.DOCTRINE_WEAK;
        }

        if (manager == DoctrineTypes.Manager.MONGO_DB || manager == DoctrineTypes.Manager.COUCH_DB) {
            return Symfony2Icons.NO_SQL;
        }

        return null;
    }

    @Nullable
    private Icon getIcon() {
        if (manager == null) {
            return null;
        }

        if (manager == DoctrineTypes.Manager.ORM) {
            return Symfony2Icons.DOCTRINE;
        }

        if (manager == DoctrineTypes.Manager.MONGO_DB || manager == DoctrineTypes.Manager.COUCH_DB) {
            return Symfony2Icons.NO_SQL;
        }

        return null;
    }
}

