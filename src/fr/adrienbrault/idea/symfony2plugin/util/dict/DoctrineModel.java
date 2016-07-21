package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoctrineModel {

    private final PhpClass phpClass;
    private String doctrineNamespace;
    private String doctrineShortcut = null;

    public DoctrineModel(@NotNull PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    public DoctrineModel(@NotNull PhpClass phpClass, @Nullable String doctrineShortcut) {
        this.phpClass = phpClass;
        this.doctrineShortcut = doctrineShortcut;
    }

    public DoctrineModel(@NotNull PhpClass phpClass, @Nullable String doctrineShortcut, @Nullable String doctrineNamespace) {
        this(phpClass, doctrineShortcut);
        this.doctrineNamespace = doctrineNamespace;
    }

    @NotNull
    public PhpClass getPhpClass() {
        return phpClass;
    }

    @Nullable
    public String getDoctrineNamespace() {
        return doctrineNamespace;
    }

    @Nullable
    public String getRepositoryName() {

        String className = phpClass.getPresentableFQN();
        if(doctrineShortcut == null) {
            return className;
        }

        if(doctrineNamespace != null && className.length() > doctrineNamespace.length()) {
            className = className.substring(doctrineNamespace.length());
            return doctrineShortcut + ':'  + className;
        }

        return null;
    }

}
