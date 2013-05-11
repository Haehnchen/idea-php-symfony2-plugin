package fr.adrienbrault.idea.symfony2plugin.config.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @link http://docs.doctrine-project.org/en/latest/reference/basic-mapping.html
 */
public class DoctrineStaticTypeLookupBuilder {

    public static ArrayList<LookupElement> getTypes() {
        return ListToElements(Arrays.asList("id", "string", "integer", "smallint", "bigint", "boolean", "decimal", "date", "time", "datetime", "text", "array", "float"));
    }

    public static ArrayList<LookupElement> getNullAble() {
        return ListToElements(Arrays.asList("true", "false"));
    }

    public static ArrayList<LookupElement> getJoinColumns() {
        return ListToElements(Arrays.asList("name", "referencedColumnName", "unique", "nullable", "onDelete", "onUpdate", "columnDefinition"));
    }

    public static ArrayList<LookupElement> getRootItems() {
        return ListToElements(Arrays.asList("type", "table", "fields", "manyToOne", "manyToMany", "oneToOne", "oneToMany", "indexes", "id", "lifecycleCallbacks", "repositoryClass", "inheritanceType", "discriminatorColumn"));
    }

    public static ArrayList<LookupElement> getAssociationMapping(Association type) {

        switch (type) {
            case oneToOne:
                return ListToElements(Arrays.asList("targetEntity", "inversedBy", "joinColumn"));

            case oneToMany:
                return ListToElements(Arrays.asList("targetEntity", "mappedBy"));

            case manyToOne:
                return ListToElements(Arrays.asList("targetEntity", "inversedBy", "joinColumn"));

            case manyToMany:
                return ListToElements(Arrays.asList("targetEntity", "inversedBy", "joinTable", "mappedBy"));

            default:
                return new ArrayList<LookupElement>();
        }

    }

    public static enum Association {
        oneToOne, oneToMany, manyToOne, manyToMany,
    }

    public static ArrayList<LookupElement> getOrmFieldAnnotations() {
        return ListToElements(Arrays.asList("@ORM\\Column", "@ORM\\GeneratedValue", "@ORM\\UniqueConstraint", "@ORM\\Id", "@ORM\\JoinTable", "@ORM\\ManyToOne", "@ORM\\ManyToMany", "@ORM\\OneToOne", "@ORM\\OneToMany"));
    }

    public static ArrayList<LookupElement> getPropertyMappings() {
        return ListToElements(Arrays.asList("type", "column", "length", "unique", "nullable", "precision", "scale", "columnDefinition"));
    }

    public static ArrayList<LookupElement> ListToElements(List<String> items) {

        ArrayList<LookupElement> lookups = new ArrayList<LookupElement>();

        for (String answer : items) {
            lookups.add(new DoctrineTypeLookup(answer));
        }

        return lookups;
    }
}
