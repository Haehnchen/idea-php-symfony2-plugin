package fr.adrienbrault.idea.symfony2plugin.translation.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAndQuickFixAction;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationKeyIntention extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        Pair<String, String> pair = getKeyAndDomain(psiElement);
        if(pair == null) {
            return;
        }

        new TranslationKeyIntentionAndQuickFixAction(pair.getFirst(), pair.getSecond(), new MyKeyDomainNotExistingCollector())
            .invoke(project, editor, psiElement.getContainingFile());
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        return getKeyAndDomain(psiElement) != null;
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getText() {
        return "Symfony: create translation key";
    }

    @Nullable
    private Pair<String, String> getKeyAndDomain(@NotNull PsiElement psiElement) {
        if(!TwigPattern.getTranslationKeyPattern("trans", "transchoice").accepts(psiElement)) {
            return null;
        }

        String key = psiElement.getText();
        if(StringUtils.isBlank(key)) {
            return null;
        }

        // get domain on file scope or method parameter
        String domainName = TwigUtil.getPsiElementTranslationDomain(psiElement);

        // inspection will take care of complete unknown key
        if(!TranslationUtil.hasTranslationKey(psiElement.getProject(), key, domainName)) {
            return null;
        }

        return Pair.create(key, domainName);
    }

    /**
     * Collect all domain files that are not providing the given key
     * Known VirtualFiles are filtered out based on the index
     */
    private static class MyKeyDomainNotExistingCollector implements TranslationKeyIntentionAndQuickFixAction.DomainCollector {
        @NotNull
        @Override
        public Collection<PsiFile> collect(@NotNull Project project, @NotNull String key, @NotNull String domain) {
            return TranslationUtil.getDomainPsiFiles(project, domain).stream()
                .filter(psiFile -> !isDomainAndKeyInPsi(psiFile, key, domain))
                .collect(Collectors.toList());
        }

        private boolean isDomainAndKeyInPsi(@NotNull PsiFile psiFile, @NotNull String key, @NotNull String domain) {
            List<Set<String>> values = FileBasedIndex.getInstance()
                .getValues(TranslationStubIndex.KEY, domain, GlobalSearchScope.fileScope(psiFile));

            for (Set<String> value : values) {
                if(value.contains(key)) {
                    return true;
                }
            }

            return false;
        }
    }
}