package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.psi.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineEntityLookupElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityReference extends PsiReferenceBase<PsiElement> implements PsiReference {
    private String entityName;

    public EntityReference(@NotNull StringLiteralExpression element) {
        super(element);

        entityName = element.getText().substring(
                element.getValueRange().getStartOffset(),
                element.getValueRange().getEndOffset()
        );
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        PhpClass entity = EntityHelper.resolveShortcutName(getElement().getProject(), this.entityName);
        if(entity != null) {
            return new PsiElementResolveResult(entity).getElement();
        }

        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());
        Collection<PhpClass> phpClasses = phpIndex.getAllSubclasses("\\Symfony\\Component\\HttpKernel\\Bundle\\Bundle");

        List<LookupElement> results = new ArrayList<LookupElement>();
        for (PhpClass phpClass : phpClasses) {

            // search for classes that match the symfony2 namings
            String ns = phpClass.getNamespaceName() + "Entity";
            Collection<PhpNamespace> entities = phpIndex.getNamespacesByName(ns);

            // @TODO: it looks like PhpIndex cant search for classes like \ns\Path\*\...
            // temporary only use flat entities and dont support "MyBundle:Folder\Entity"
            for (PhpNamespace entity_files : entities) {

                // build our symfony2 shortcut
                System.out.println(entity_files.getContainingFile().getContainingDirectory());
                String filename = entity_files.getContainingFile().getName();
                String className = filename.substring(0, filename.lastIndexOf('.'));
                String repoName = phpClass.getName() + ':'  + className;

                for (PhpClass entity_phpclass : phpIndex.getClassesByFQN(ns + "\\" + className)) {
                    results.add(new DoctrineEntityLookupElement(repoName, entity_phpclass));
                }

              }

        }

        return results.toArray();
    }

}
