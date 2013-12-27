package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

public interface DoctrineTypes {
    public static final String REPOSITORY_INTERFACE = "\\Doctrine\\Common\\Persistence\\ObjectRepository";

    public enum Manager {
        ORM, ODM
    }

}
