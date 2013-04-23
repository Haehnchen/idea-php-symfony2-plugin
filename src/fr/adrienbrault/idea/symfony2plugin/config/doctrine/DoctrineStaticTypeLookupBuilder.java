package fr.adrienbrault.idea.symfony2plugin.config.doctrine;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DoctrineStaticTypeLookupBuilder {

    public static ArrayList<LookupElement> getTypes() {
        return ListToElements(Arrays.asList("string", "integer", "smallint", "bigint", "boolean", "decimal", "date", "time", "datetime", "text", "array", "float"));
    }

    public static ArrayList<LookupElement> getNullAble() {
        return ListToElements(Arrays.asList("true", "false"));
    }

    public static ArrayList<LookupElement> getJoinColumns() {
        return ListToElements(Arrays.asList("name", "referencedColumnName", "unique", "nullable", "onDelete", "onUpdate", "columnDefinition"));
    }

    public static ArrayList<LookupElement> ListToElements(List<String> items) {

        ArrayList<LookupElement> lookups = new ArrayList<LookupElement>();

        for (String answer : items) {
            lookups.add(new DoctrineTypeLookup(answer));
        }

        return lookups;
    }
}
