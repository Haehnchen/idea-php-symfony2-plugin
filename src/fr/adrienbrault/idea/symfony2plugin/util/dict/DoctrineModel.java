package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.Nullable;

public class DoctrineModel {

    private final PhpClass phpClass;
    private String doctrineNamespace;
    private final String doctrineShortcut;

    public DoctrineModel(PhpClass phpClass, String doctrineShortcut) {
        this.phpClass = phpClass;
        this.doctrineShortcut = doctrineShortcut;
    }

    public DoctrineModel(PhpClass phpClass, String doctrineShortcut, String doctrineNamespace) {
        this(phpClass, doctrineShortcut);
        this.doctrineNamespace = doctrineNamespace;
    }

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

        if(className != null && className.length() > doctrineNamespace.length()) {
            className = className.substring(doctrineNamespace.length());
            return doctrineShortcut + ':'  + className;
        }

        return null;
    }

}
