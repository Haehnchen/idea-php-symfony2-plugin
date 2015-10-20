package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dbal;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineDbalQbGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {

        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {

                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                if (!isTablaNameRegistrar(context)) {
                    return null;
                }

                return new DbalTableGotoCompletionProvider(context);
            }
        });

    }

    private boolean isTablaNameRegistrar(PsiElement context) {

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "update")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "insert")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "from")
            .match();

        if(methodMatchParameter != null) {
            return true;
        }

        methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 1)
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "innerJoin")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "leftJoin")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "join")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "rightJoin")
            .match();

        return methodMatchParameter != null;

    }

    private static class DbalTableGotoCompletionProvider extends GotoCompletionProvider {

        public DbalTableGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> elements = new ArrayList<LookupElement>();

            for (Pair<String, PsiElement> pair : DoctrineMetadataUtil.getTables(getProject())) {
                elements.add(LookupElementBuilder.create(pair.getFirst()).withIcon(Symfony2Icons.DOCTRINE));
            }

            return elements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {

            PsiElement parent = element.getParent();
            if(!(parent instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            String contents = ((StringLiteralExpression) parent).getContents();
            if(StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

            for (Pair<String, PsiElement> pair : DoctrineMetadataUtil.getTables(getProject())) {
                if(!contents.equals(pair.getFirst())) {
                    continue;
                }

                PsiElement second = pair.getSecond();
                if(second == null) {
                    continue;
                }

                psiElements.add(second);
            }

            return psiElements;
        }
    }
}
