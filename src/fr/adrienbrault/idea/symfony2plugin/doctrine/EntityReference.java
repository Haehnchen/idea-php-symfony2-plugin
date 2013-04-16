package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.psi.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
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

        Symfony2ProjectComponent symfony2ProjectComponent = getElement().getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String, String> em = symfony2ProjectComponent.getEntityNamespacesMap();

        List<LookupElement> results = new ArrayList<LookupElement>();
        for (String shortcutName : em.keySet()) {

            // search for classes that match the symfony2 namings
            Collection<PhpNamespace> entities = phpIndex.getNamespacesByName(em.get(shortcutName));

            // @TODO: it looks like PhpIndex cant search for classes like \ns\Path\*\...
            // temporary only use flat entities and dont support "MyBundle:Folder\Entity"
            for (PhpNamespace entity_files : entities) {

                // build our symfony2 shortcut
                String filename = entity_files.getContainingFile().getName();
                String className = filename.substring(0, filename.lastIndexOf('.'));
                String repoName = shortcutName + ':'  + className;

                // dont add Repository classes and abstract entities
                if(!className.endsWith("Repository") && !className.equals("Repository")) {
                    for (PhpClass entityClass : phpIndex.getClassesByFQN(em.get(shortcutName) + "\\" + className)) {
                        if(!entityClass.isAbstract()) {
                            results.add(new DoctrineEntityLookupElement(repoName, entityClass));
                        }
                    }
                }

              }

        }

        return results.toArray();
    }

}
