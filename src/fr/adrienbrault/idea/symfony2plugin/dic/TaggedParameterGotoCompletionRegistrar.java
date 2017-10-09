package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TaggedParameterGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // arguments: [!tagged twig.extension]
        // <argument type="tagged" tag="foobar" />
        registrar.register(
            PlatformPatterns.or(
                YamlElementPatternHelper.getTaggedServicePattern(),
                XmlPatterns.psiElement().withParent(XmlHelper.getTypeTaggedTagAttribute())
            ),
            new MyTagGotoCompletionContributor()
        );
    }

    private static class MyTagGotoCompletionContributor implements GotoCompletionContributor {
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            return new GotoCompletionProvider(psiElement) {
                @NotNull
                @Override
                public Collection<LookupElement> getLookupElements() {
                    return TagNameCompletionProvider.getTagLookupElements(getProject());
                }

                @NotNull
                @Override
                public Collection<PsiElement> getPsiTargets(PsiElement element) {
                    String tagName = GotoCompletionUtil.getTextValueForElement(element);
                    if(tagName == null) {
                        return Collections.emptyList();
                    }

                    return new ArrayList<>(ServiceUtil.getTaggedClasses(element.getProject(), tagName));
                }
            };
        }
    }
}
