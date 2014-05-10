package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderPropertyAlias;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderRelation;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.processor.QueryBuilderChainProcessor;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util.MatcherUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryBuilderCompletionContributor extends CompletionContributor {

    private static MethodMatcher.CallToSignature[] JOINS = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "join"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "leftJoin"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "rightJoin"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "innerJoin"),
    };

    public QueryBuilderCompletionContributor() {

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 0)
                    .withSignature("\\Doctrine\\ORM\\QueryBuilder", "setParameter")
                    .match();

                if(methodMatchParameter == null) {
                    methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(psiElement.getContext(), 0)
                        .withSignature("\\Doctrine\\ORM\\QueryBuilder", "setParameters")
                        .match();
                }

                if(methodMatchParameter == null) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                if(qb == null) {
                    return;
                }

                for(String parameter: qb.collect().getParameters()) {
                    completionResultSet.addElement(LookupElementBuilder.create(parameter));
                }

            }

        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 0)
                    .withSignature(JOINS)
                    .match();

                if(methodMatchParameter == null) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                if(qb == null) {
                    return;
                }

                QueryBuilderScopeContext collect = qb.collect();
                for(Map.Entry<String, List<QueryBuilderRelation>> parameter: collect.getRelationMap().entrySet()) {
                    for(QueryBuilderRelation relation: parameter.getValue()) {
                        completionResultSet.addElement(LookupElementBuilder.create(parameter.getKey() + "." + relation.getFieldName()).withTypeText(relation.getTargetEntity(), true));
                    }
                }

            }

        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = MatcherUtil.matchPropertyField(psiElement.getContext());
                if(methodMatchParameter == null) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                if(qb == null) {
                    return;
                }

                QueryBuilderScopeContext collect = qb.collect();
                for(Map.Entry<String, QueryBuilderPropertyAlias> entry: collect.getPropertyAliasMap().entrySet()) {

                    LookupElementBuilder lookup = LookupElementBuilder.create(entry.getKey());
                    lookup = lookup.withIcon(Symfony2Icons.DOCTRINE);
                    if(entry.getValue().getField() != null) {
                        lookup = lookup.withTypeText(entry.getValue().getField().getTypeName(), true);

                        if(entry.getValue().getField().getRelationType() != null) {
                            lookup = lookup.withTailText(entry.getValue().getField().getRelationType(), true);
                            lookup = lookup.withTypeText(entry.getValue().getField().getRelation(), true);
                            lookup = lookup.withIcon(PhpIcons.CLASS_ICON);
                        }

                    }

                    // highlight fields which are possible in select statement
                    if(collect.getSelects().contains(entry.getValue().getAlias())) {
                        lookup = lookup.withBoldness(true);
                    }

                    completionResultSet.addElement(lookup);

                }

            }

        });

        // $qb->join('test.foo', 'foo');
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 1)
                    .withSignature(JOINS)
                    .match();

                if(methodMatchParameter != null) {
                    MethodReference methodReference = PsiTreeUtil.getParentOfType(psiElement, MethodReference.class);
                    if(methodReference != null) {
                        String joinTable = PhpElementsUtil.getStringValue(PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0));
                        if(joinTable != null && StringUtils.isNotBlank(joinTable)) {
                            int pos = joinTable.lastIndexOf(".");
                            if(pos > 0) {
                                final String aliasName = joinTable.substring(pos + 1);
                                if(StringUtils.isNotBlank(aliasName)) {

                                    Set<String> strings = new HashSet<String>() {{
                                        add(aliasName);
                                        add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(aliasName, true));
                                        add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(aliasName));
                                    }};

                                    for(String string: strings) {
                                        completionResultSet.addElement(LookupElementBuilder.create(string));
                                    }

                                }
                            }
                        }
                    }
                }

            }

        });

    }

    @Nullable
    public static QueryBuilderMethodReferenceParser getQueryBuilderParser(MethodReference psiElement) {
        QueryBuilderChainProcessor queryBuilderRecursiveProcessor = new QueryBuilderChainProcessor(psiElement);
        final List<MethodReference> methodReferences = queryBuilderRecursiveProcessor.collectMethods();

        return new QueryBuilderMethodReferenceParser(psiElement.getProject(), methodReferences);

    }

}
