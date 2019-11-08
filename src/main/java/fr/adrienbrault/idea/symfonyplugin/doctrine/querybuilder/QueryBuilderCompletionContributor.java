package fr.adrienbrault.idea.symfonyplugin.doctrine.querybuilder;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfonyplugin.doctrine.querybuilder.dict.QueryBuilderPropertyAlias;
import fr.adrienbrault.idea.symfonyplugin.doctrine.querybuilder.dict.QueryBuilderRelation;
import fr.adrienbrault.idea.symfonyplugin.doctrine.querybuilder.processor.QueryBuilderChainProcessor;
import fr.adrienbrault.idea.symfonyplugin.doctrine.querybuilder.util.MatcherUtil;
import fr.adrienbrault.idea.symfonyplugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderCompletionContributor extends CompletionContributor {

    private static MethodMatcher.CallToSignature[] JOINS = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "join"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "leftJoin"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "rightJoin"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "innerJoin"),
    };

    private static MethodMatcher.CallToSignature[] WHERES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "where"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "andWhere"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "orWhere"),
    };

    // mmh... really that good; not added all because of performance? :)
    public static MethodMatcher.CallToSignature[] EXPR = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "andX"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "orX"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "eq"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "neq"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "lt"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "lte"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "gt"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "gte"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "avg"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "max"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "min"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "count"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "diff"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "sum"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "quot"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "in"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "notIn"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "like"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "notLike"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "concat"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query\\Expr", "between"),
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
                if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
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
                buildLookupElements(completionResultSet, collect);

            }

        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 0)
                    .withSignature(WHERES)
                    .match();

                if(methodMatchParameter == null) {
                    return;
                }

                // querybuilder parser is too slow longer values, and that dont make sense here at all
                // user can fire a manual completion event, when needed...
                if(completionParameters.isAutoPopup()) {
                    if(psiElement instanceof StringLiteralExpression) {
                        if(((StringLiteralExpression) psiElement).getContents().length() > 5) {
                            return;
                        }
                    }
                }

                // $qb->andWhere('foo.id = ":foo_id"')
                addParameterNameCompletion(completionParameters, completionResultSet, psiElement);

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                if(qb == null) {
                    return;
                }

                QueryBuilderScopeContext collect = qb.collect();
                buildLookupElements(completionResultSet, collect);

            }

            private void addParameterNameCompletion(CompletionParameters completionParameters, CompletionResultSet completionResultSet, PsiElement psiElement) {

                PsiElement literalExpr = psiElement.getParent();
                if(!(literalExpr instanceof StringLiteralExpression)) {
                    return;
                }

                String content = PsiElementUtils.getStringBeforeCursor((StringLiteralExpression) literalExpr, completionParameters.getOffset());
                if(content == null) {
                    return;
                }

                Matcher matcher = Pattern.compile("(\\w+)\\.(\\w+)[\\s+]*[=><]+[\\s+]*$").matcher(content);
                if (matcher.find()) {
                    final String complete = matcher.group(1) + "_" + matcher.group(2);

                    // fill underscore and underscore completion
                    Set<String> strings = new HashSet<String>() {{
                        add(complete);
                        add(fr.adrienbrault.idea.symfonyplugin.util.StringUtils.camelize(complete, true));
                    }};

                    for(String string: strings) {
                        completionResultSet.addElement(LookupElementBuilder.create(":" + string).withIcon(Symfony2Icons.DOCTRINE));
                    }

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
                                        add(fr.adrienbrault.idea.symfonyplugin.util.StringUtils.camelize(aliasName, true));
                                        add(fr.adrienbrault.idea.symfonyplugin.util.StringUtils.underscore(aliasName));
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

        // $qb->expr()->in('')
        // $qb->expr()->eg('')
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 0)
                    .withSignature(EXPR)
                    .match();

                if(methodMatchParameter == null) {
                    return;
                }

                // simple resolve query inline instance usage
                // $qb->expr()->in('')
                MethodReference methodReference = methodMatchParameter.getMethodReference();
                PsiElement methodReferenceChild = methodReference.getFirstChild();
                if(!(methodReferenceChild instanceof MethodReference)) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser((MethodReference) methodReferenceChild);
                if(qb == null) {
                    return;
                }

                QueryBuilderScopeContext collect = qb.collect();
                buildLookupElements(completionResultSet, collect);

            }

        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 2)
                    .withSignature("\\Doctrine\\ORM\\QueryBuilder", "from")
                    .match();

                if(methodMatchParameter == null) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                if(qb == null) {
                    return;
                }

                QueryBuilderScopeContext collect = qb.collect();
                buildLookupElements(completionResultSet, collect);

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
                    .withSignature("\\Doctrine\\ORM\\EntityRepository", "createQueryBuilder")
                    .match();

                if(methodMatchParameter == null) {
                    return;
                }

                for(String type: methodMatchParameter.getMethodReference().getType().getTypes()) {

                    // strip last method call
                    if(type.endsWith(".createQueryBuilder"))  {
                        attachClassNames(completionResultSet, type.substring(0, type.length() - ".createQueryBuilder".length()));
                    }

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

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 1)
                    .withSignature("\\Doctrine\\ORM\\QueryBuilder", "from")
                    .match();

                if(methodMatchParameter == null) {
                    return;
                }

                MethodReference methodReference = methodMatchParameter.getMethodReference();
                String repoName = PhpElementsUtil.getStringValue(methodReference.getParameters()[0]);
                if(repoName != null) {
                    attachClassNames(completionResultSet, repoName);
                }

            }
        });

    }

    private void attachClassNames(@NotNull CompletionResultSet completionResultSet, @NotNull String repoName) {
        // FooBarRepository => FooBar
        if(repoName.toLowerCase().endsWith("repository")) {
            repoName = repoName.substring(0, repoName.length() - "repository".length());
        }

        int endIndex = repoName.lastIndexOf(":");
        if(endIndex == -1) {
            endIndex = repoName.lastIndexOf("\\");
        }

        if(endIndex > 0) {
            // unique list for equal underscore or camelize
            Set<String> strings = new HashSet<>();

            String underscore = fr.adrienbrault.idea.symfonyplugin.util.StringUtils.underscore(repoName.substring(endIndex + 1, repoName.length()));

            // foo_bar => fb
            List<String> starting = new ArrayList<>();
            for (String s : underscore.split("_")) {
                if(s.length() > 0) {
                    starting.add(s.substring(0, 1));
                }
            }

            strings.add(StringUtils.join(starting, ""));
            strings.add(underscore);
            strings.add(fr.adrienbrault.idea.symfonyplugin.util.StringUtils.camelize(strings.iterator().next(), true));

            for(String lookup: strings) {
                completionResultSet.addElement(LookupElementBuilder.create(lookup));
            }
        }
    }

    private void buildLookupElements(CompletionResultSet completionResultSet, QueryBuilderScopeContext collect) {
        for(Map.Entry<String, QueryBuilderPropertyAlias> entry: collect.getPropertyAliasMap().entrySet()) {
            DoctrineModelField field = entry.getValue().getField();
            LookupElementBuilder lookup = LookupElementBuilder.create(entry.getKey());
            lookup = lookup.withIcon(Symfony2Icons.DOCTRINE);
            if(field != null) {
                lookup = lookup.withTypeText(field.getTypeName(), true);

                if(field.getRelationType() != null) {
                    lookup = lookup.withTailText("(" + field.getRelationType() + ")", true);
                    lookup = lookup.withTypeText(field.getRelation(), true);
                    lookup = lookup.withIcon(PhpIcons.CLASS_ICON);
                } else {
                    // relation tail text wins
                    String column = field.getColumn();
                    if(column != null) {
                        lookup = lookup.withTailText("(" + column + ")", true);
                    }
                }

            }

            // highlight fields which are possible in select statement
            if(collect.getSelects().contains(entry.getValue().getAlias())) {
                lookup = lookup.withBoldness(true);
            }

            completionResultSet.addElement(lookup);

        }
    }

    @Nullable
    public static QueryBuilderMethodReferenceParser getQueryBuilderParser(MethodReference methodReference) {
        final QueryBuilderChainProcessor processor = new QueryBuilderChainProcessor(methodReference);
        processor.collectMethods();

        // @TODO: pipe factory method
        return new QueryBuilderMethodReferenceParser(methodReference.getProject(), new ArrayList<MethodReference>() {{
            addAll(processor.getQueryBuilderFactoryMethods());
            addAll(processor.getQueryBuilderMethodReferences());
        }});

    }

}
