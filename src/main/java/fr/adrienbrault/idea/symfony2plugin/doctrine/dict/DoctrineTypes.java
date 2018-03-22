package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
final public class DoctrineTypes {

    public static final String REPOSITORY_INTERFACE = "\\Doctrine\\Common\\Persistence\\ObjectRepository";

    public enum Manager {
        ORM, MONGO_DB, COUCH_DB
    }

    public static Map<Manager, String> getManagerInstanceMap() {
        Map<Manager, String> managerMap = new HashMap<>();
        managerMap.put(Manager.ORM, "\\Doctrine\\ORM\\EntityManager");
        managerMap.put(Manager.MONGO_DB, "\\Doctrine\\ODM\\MongoDB\\DocumentManager");
        managerMap.put(Manager.COUCH_DB, "\\Doctrine\\ODM\\CouchDB\\DocumentManager");
        return managerMap;
    }

}
