package fr.adrienbrault.idea.symfony2plugin.config.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationMethodInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationTagInsertHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @link http://docs.doctrine-project.org/en/latest/reference/basic-mapping.html
 */
public class DoctrineStaticTypeLookupBuilder {

    private InsertHandler insertHandler = InsertHandler.Yaml;

    public static void fillOrmLookupElementsWithStatic(@NotNull Collection<LookupElement> lookupElements) {

        List<String> strings = Arrays.asList("id", "string", "integer", "smallint", "bigint", "boolean", "decimal", "date", "time", "datetime", "text", "array", "float");

        Collection<String> lookupStrings = ContainerUtil.map(lookupElements, LookupElement::getLookupString);

        for (String string : strings) {
            if(!lookupStrings.contains(string)) {
                lookupElements.add(LookupElementBuilder.create(string).withIcon(Symfony2Icons.DOCTRINE));
            }
        }
    }

    public ArrayList<LookupElement> getNullAble() {
        return ListToElements(Arrays.asList("true", "false"));
    }

    public ArrayList<LookupElement> getRootItems() {
        return ListToElements(Arrays.asList("type", "table", "fields", "embedded", "manyToOne", "manyToMany", "oneToOne", "oneToMany", "indexes", "id", "lifecycleCallbacks", "repositoryClass", "inheritanceType", "discriminatorColumn", "uniqueConstraints"));
    }

    public enum InsertHandler {
        Annotations, Yaml
    }

    public ArrayList<LookupElement> ListToElements(List<String> items) {

        // @TODO: not nice here
        if(this.insertHandler.equals(InsertHandler.Annotations)) {
            return this.ListToAnnotationsElements(items);
        }

        return this.ListToYmlElements(items);
    }

    public ArrayList<LookupElement> ListToAnnotationsElements(List<String> items) {

        ArrayList<LookupElement> lookups = new ArrayList<>();

        for (String item : items) {
            if(item.startsWith("@")) {
                lookups.add(new DoctrineTypeLookup(item, AnnotationTagInsertHandler.getInstance()));
            } else {
                lookups.add(new DoctrineTypeLookup(item, AnnotationMethodInsertHandler.getInstance()));
            }

        }

        return lookups;
    }

    public ArrayList<LookupElement> ListToYmlElements(List<String> items) {

        ArrayList<LookupElement> lookups = new ArrayList<>();

        for (String answer : items) {
            lookups.add(new DoctrineTypeLookup(answer));
        }

        return lookups;

    }

}
