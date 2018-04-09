package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineXmlGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {

        registrar.register(XmlPatterns.psiElement().withParent(PlatformPatterns.or(
            DoctrineMetadataPattern.getXmlModelClass(),
            DoctrineMetadataPattern.getXmlRepositoryClass(),
            DoctrineMetadataPattern.getXmlTargetDocumentClass(),
            DoctrineMetadataPattern.getXmlTargetEntityClass(),
            DoctrineMetadataPattern.getEmbeddableNameClassPattern()
        )), ClassGotoCompletionProvider::new);
    }

    private static class ClassGotoCompletionProvider extends GotoCompletionProvider {

        public ClassGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {

            PsiElement parent = element.getParent();
            if(!(parent instanceof XmlAttributeValue)) {
                return Collections.emptyList();
            }

            String value = ((XmlAttributeValue) parent).getValue();
            if(StringUtils.isBlank(value)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> classes = new ArrayList<>();
            for (PhpClass phpClass : DoctrineMetadataUtil.getClassInsideScope(element, value)) {
                classes.add(phpClass);
            }

            return classes;
        }
    }
}
