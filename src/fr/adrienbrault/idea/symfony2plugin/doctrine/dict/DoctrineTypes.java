package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import java.util.HashMap;
import java.util.Map;

final public class DoctrineTypes {

    public static final String REPOSITORY_INTERFACE = "\\Doctrine\\Common\\Persistence\\ObjectRepository";

    public enum Manager {
        ORM, MONGO_DB
    }

    public static Map<Manager, String> getManagerInstanceMap() {
        Map<Manager, String> managerMap = new HashMap<Manager, String>();
        managerMap.put(Manager.ORM, "\\Doctrine\\ORM\\EntityManager");
        managerMap.put(Manager.MONGO_DB, "\\Doctrine\\Bundle\\MongoDBBundle\\ManagerRegistry");
        return managerMap;
    }

}
