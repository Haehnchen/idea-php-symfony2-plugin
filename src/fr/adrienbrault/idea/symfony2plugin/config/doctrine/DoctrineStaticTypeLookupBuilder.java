package fr.adrienbrault.idea.symfony2plugin.config.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationMethodInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationTagInsertHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @link http://docs.doctrine-project.org/en/latest/reference/basic-mapping.html
 */
public class DoctrineStaticTypeLookupBuilder {

    private InsertHandler insertHandler = InsertHandler.Yaml;

    public static Collection<LookupElement> getTypes(@NotNull Project project) {

        final Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

        visitCustomTypes(project, new ColumnTypeVisitor() {
            @Override
            public void visit(@NotNull String name, @Nullable PhpClass phpClass, @Nullable PsiElement psiElement) {
                LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(name).withIcon(Symfony2Icons.DOCTRINE);

                if(phpClass != null) {
                    lookupElementBuilder = lookupElementBuilder.withTypeText(phpClass.getName(), true);
                }

                lookupElements.add(lookupElementBuilder);
            }
        });

        return lookupElements;
    }

    public static void visitCustomTypes(@NotNull Project project, @NotNull ColumnTypeVisitor visitor) {

        Set<String> found = new HashSet<String>();

        for (PhpClass phpClass : PhpIndex.getInstance(project).getAllSubclasses("\\Doctrine\\DBAL\\Types\\Type")) {
            String name = PhpElementsUtil.getMethodReturnAsString(phpClass, "getName");
            if(name != null) {
                found.add(name);
                visitor.visit(name, phpClass, phpClass.findMethodByName("getName"));
            }
        }

        for (String s : Arrays.asList("id", "string", "integer", "smallint", "bigint", "boolean", "decimal", "date", "time", "datetime", "text", "array", "float")) {
            if(!found.contains(s)) {
                visitor.visit(s, null, null);
            }
        }

    }

    private interface ColumnTypeVisitor {
        void visit(@NotNull String name, @Nullable PhpClass phpClass, @Nullable PsiElement psiElement);
    }

    public static Collection<PsiElement> getColumnTypesTargets(@NotNull Project project, final @NotNull String contents) {

        final Collection<PsiElement> targets = new ArrayList<PsiElement>();

        visitCustomTypes(project, new ColumnTypeVisitor() {
            @Override
            public void visit(@NotNull String name, @Nullable PhpClass phpClass, @Nullable PsiElement psiElement) {
                if(name.equals(contents) && phpClass != null) {
                    targets.add(phpClass);
                }
            }
        });

        return targets;
    }

    public ArrayList<LookupElement> getNullAble() {
        return ListToElements(Arrays.asList("true", "false"));
    }

    public ArrayList<LookupElement> getRootItems() {
        return ListToElements(Arrays.asList("type", "table", "fields", "manyToOne", "manyToMany", "oneToOne", "oneToMany", "indexes", "id", "lifecycleCallbacks", "repositoryClass", "inheritanceType", "discriminatorColumn", "uniqueConstraints"));
    }

    public static enum InsertHandler {
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

        ArrayList<LookupElement> lookups = new ArrayList<LookupElement>();

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

        ArrayList<LookupElement> lookups = new ArrayList<LookupElement>();

        for (String answer : items) {
            lookups.add(new DoctrineTypeLookup(answer));
        }

        return lookups;

    }

}
