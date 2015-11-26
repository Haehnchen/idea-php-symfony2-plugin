package fr.adrienbrault.idea.symfony2plugin.completion.xml;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class XmlGotoCompletionRegistrar implements GotoCompletionRegistrar  {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {

        // <import resource="config_foo.xml"/>
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getImportResourcePattern()), new GotoCompletionContributor() {
                @Nullable
                @Override
                public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
                    return new ImportResourceGotoCompletionProvider(psiElement);
                }
            }
        );
    }

    private static class ImportResourceGotoCompletionProvider extends GotoCompletionProvider {

        public ImportResourceGotoCompletionProvider(PsiElement element) {
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
            String xmlAttributeValue = GotoCompletionUtil.getXmlAttributeValue(element);
            if(xmlAttributeValue == null) {
                return Collections.emptyList();
            }

            Collection<PsiElement> targets = new ArrayList<PsiElement>();

            targets.addAll(FileResourceUtil.getFileResourceTargetsInBundleScope(element.getProject(), xmlAttributeValue));

            PsiFile containingFile = element.getContainingFile();
            if(containingFile != null) {
                targets.addAll(FileResourceUtil.getFileResourceTargetsInDirectoryScope(containingFile, xmlAttributeValue));
            }

            return targets;
        }
    }
}
