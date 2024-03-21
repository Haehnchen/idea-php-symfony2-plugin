package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderPropertyAlias;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderRelation;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.processor.QueryBuilderChainProcessor;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util.MatcherUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderCompletionContributor extends CompletionContributor {

    private static final MethodMatcher.CallToSignature[] JOINS = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "join"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "leftJoin"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "rightJoin"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "innerJoin"),
    };

    private static final MethodMatcher.CallToSignature[] WHERES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "where"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "andWhere"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "orWhere"),
    };

    // mmh... really that good; not added all because of performance? :)
    public static final MethodMatcher.CallToSignature[] EXPR = new MethodMatcher.CallToSignature[] {
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
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 0)
                    .withSignature("\\Doctrine\\ORM\\QueryBuilder", "setParameter")
                    .match();

                if (methodMatchParameter == null) {
                    methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(psiElement.getContext(), 0)
                        .withSignature("\\Doctrine\\ORM\\QueryBuilder", "setParameters")
                        .match();
                }

                if (methodMatchParameter == null) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                for (String parameter : qb.collect().getParameters()) {
                    completionResultSet.addElement(LookupElementBuilder.create(parameter).withIcon(Symfony2Icons.DOCTRINE));
                }
            }
        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 0)
                    .withSignature(JOINS)
                    .match();

                if (methodMatchParameter == null) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                QueryBuilderScopeContext collect = qb.collect();
                for (Map.Entry<String, List<QueryBuilderRelation>> parameter : collect.getRelationMap().entrySet()) {
                    for (QueryBuilderRelation relation : parameter.getValue()) {
                        LookupElementBuilder element = LookupElementBuilder
                            .create(parameter.getKey() + "." + relation.getFieldName())
                            .withIcon(Symfony2Icons.DOCTRINE)
                            .withTypeText(StringUtils.stripStart(relation.getTargetEntity(), "\\"), true);

                        completionResultSet.addElement(element);
                    }
                }
            }
        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (psiElement == null) {
                    return;
                }

                Project project = psiElement.getProject();
                if (!Symfony2ProjectComponent.isEnabled(project)) {
                    return;
                }

                PsiElement parent = psiElement.getParent();
                if (!(parent instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = MatcherUtil.matchField(parent);
                if (methodMatchParameter == null) {
                    return;
                }

                String content = PsiElementUtils.getStringBeforeCursor((StringLiteralExpression) parent, completionParameters.getOffset());

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                QueryBuilderScopeContext collect = qb.collect();
                buildLookupElements(completionResultSet, collect);


                // "test.test"
                if (content == null || content.isBlank() || content.matches("^[\\w+.]+$")) {
                    buildLookupElements(completionResultSet, collect);
                    return;
                }

                // "foo test.test"
                Matcher matcher = Pattern.compile("(\\w+)\\.(\\w+)$").matcher(content);
                if (matcher.find())  {
                    String table = matcher.group(1);
                    String field = matcher.group(2);

                    buildLookupElements(completionResultSet.withPrefixMatcher(table + "." + field), collect);
                }

                // "foo test."
                Matcher matcher2 = Pattern.compile("(\\w+)\\.$").matcher(content);
                if (matcher2.find())  {
                    String prefix = matcher2.group(1) + ".";
                    buildLookupElements(completionResultSet.withPrefixMatcher(prefix), collect);
                }

                // "foo, test.test"
                // "(test.test"
                Matcher matcher3 = Pattern.compile("[(|,]\\s*(\\w+)$").matcher(content);
                if (matcher3.find())  {
                    String prefix = matcher3.group(1);
                    buildLookupElements(completionResultSet.withPrefixMatcher(prefix), collect);
                }

                for (Map.Entry<String, String> entry : DoctrineUtil.getDoctrineOrmFunctions(project).entrySet()) {
                    LookupElementBuilder lookup = LookupElementBuilder.create(entry.getKey().toUpperCase())
                        .withTypeText("FunctionNode")
                        .withIcon(Symfony2Icons.DOCTRINE_WEAK);

                    completionResultSet.addElement(lookup);
                }
            }
        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext())
                    .withSignature(WHERES)
                    .match();

                if (methodMatchParameter == null) {
                    return;
                }

                PsiElement parent1 = psiElement.getParent();
                if (!(parent1 instanceof StringLiteralExpression parent)) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                QueryBuilderScopeContext collect = qb.collect();

                String content = PsiElementUtils.getStringBeforeCursor(parent, completionParameters.getOffset());
                if (content == null || content.isBlank() || content.matches("^[\\w+.]+$")) {
                    buildLookupElements(completionResultSet, collect);
                    return;
                }

                Matcher matcher = Pattern.compile("(\\w+)\\.(\\w+)$").matcher(content);
                if (matcher.find())  {
                    String table = matcher.group(1);
                    String field = matcher.group(2);

                    buildLookupElements(completionResultSet.withPrefixMatcher(table + "." + field), collect);
                }

                Matcher matcher2 = Pattern.compile("(\\w+)\\.$").matcher(content);
                if (matcher2.find())  {
                    String prefix = matcher2.group(1) + ".";
                    buildLookupElements(completionResultSet.withPrefixMatcher(prefix), collect);
                }

                Matcher matcher3 = Pattern.compile("(AND|OR|WHERE|NOT|[!=><]+)\\s+(\\w+)$").matcher(content);
                if (matcher3.find())  {
                    String prefix = matcher3.group(2);
                    buildLookupElements(completionResultSet.withPrefixMatcher(prefix), collect);
                }

                // $qb->andWhere('foo.id = ":foo_id"')
                addParameterNameCompletion(completionResultSet.withPrefixMatcher(""), content);
            }

            private void addParameterNameCompletion(CompletionResultSet completionResultSet, String content) {
                // test.test = :
                // test.test =
                Matcher matcher = Pattern.compile("(\\w+)\\.(\\w+)[\\s+]*[!=><]+[\\s+]*(?<colon>:*)$").matcher(content);
                boolean hasMatch = matcher.find();

                if (!hasMatch) {
                    // test.test = :a
                    matcher = Pattern.compile("(\\w+)\\.(\\w+)[\\s+]*[!=><]+[\\s+]*(?<colon>:)(?<ident>\\w+)$").matcher(content);
                    hasMatch = matcher.find();
                    if (hasMatch) {
                        completionResultSet = completionResultSet.withPrefixMatcher(":" + matcher.group("ident"));
                    }
                } else {
                    String group = matcher.group("colon");
                    if (group == null || group.isBlank()) {
                        completionResultSet = completionResultSet.withPrefixMatcher("");
                    } else {
                        completionResultSet = completionResultSet.withPrefixMatcher(":");
                    }
                }

                if (hasMatch) {
                    final String complete = matcher.group(1) + "_" + matcher.group(2);

                    // fill underscore and underscore completion
                    Set<String> strings = new HashSet<>();

                    strings.add(complete);
                    strings.add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(complete, true));
                    strings.add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(matcher.group(2)));
                    strings.add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(matcher.group(2), true));

                    for (String string : strings) {
                        LookupElementBuilder parameter;

                        String group = matcher.group("colon");
                        if (group == null || group.isBlank()) {
                            parameter = LookupElementBuilder.create(":" + string)
                                .withIcon(Symfony2Icons.DOCTRINE)
                                .withTypeText("parameter")
                                .withPresentableText(":" + string);
                        } else {
                            parameter = LookupElementBuilder.create(":" + string)
                                .withIcon(Symfony2Icons.DOCTRINE)
                                .withTypeText("parameter")
                                .withPresentableText(":" + string);
                        }

                        completionResultSet.addElement(parameter);
                    }
                }
            }
        });

        // $qb->join('test.foo', 'foo');
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 1)
                    .withSignature(JOINS)
                    .match();

                if (methodMatchParameter != null) {
                    MethodReference methodReference = PsiTreeUtil.getParentOfType(psiElement, MethodReference.class);
                    if (methodReference != null) {
                        String joinTable = PhpElementsUtil.getStringValue(PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0));
                        if (StringUtils.isNotBlank(joinTable)) {
                            int pos = joinTable.lastIndexOf(".");
                            if (pos > 0) {
                                final String aliasName = joinTable.substring(pos + 1);
                                if (StringUtils.isNotBlank(aliasName)) {

                                    Set<String> strings = new HashSet<>() {{
                                        add(aliasName);
                                        add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(aliasName, true));
                                        add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(aliasName));
                                    }};

                                    for (String string : strings) {
                                        completionResultSet.addElement(LookupElementBuilder.create(string).withIcon(Symfony2Icons.DOCTRINE));
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
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 0)
                    .withSignature(EXPR)
                    .match();

                if (methodMatchParameter == null) {
                    return;
                }

                // simple resolve query inline instance usage
                // $qb->expr()->in('')
                MethodReference methodReference = methodMatchParameter.getMethodReference();
                PsiElement methodReferenceChild = methodReference.getFirstChild();
                if (!(methodReferenceChild instanceof MethodReference)) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser((MethodReference) methodReferenceChild);
                QueryBuilderScopeContext collect = qb.collect();
                buildLookupElements(completionResultSet, collect);
            }
        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 2)
                    .withSignature("\\Doctrine\\ORM\\QueryBuilder", "from")
                    .match();

                if (methodMatchParameter == null) {
                    return;
                }

                QueryBuilderMethodReferenceParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                QueryBuilderScopeContext collect = qb.collect();
                buildLookupElements(completionResultSet, collect);
            }
        });


        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 0)
                    .withSignature("\\Doctrine\\ORM\\EntityRepository", "createQueryBuilder")
                    .match();

                if (methodMatchParameter == null) {
                    return;
                }

                for (String type : methodMatchParameter.getMethodReference().getType().getTypes()) {

                    // strip last method call
                    if (type.endsWith(".createQueryBuilder")) {
                        attachClassNames(completionResultSet, type.substring(0, type.length() - ".createQueryBuilder".length()));
                    }

                }
            }
        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                    return;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement.getContext(), 1)
                    .withSignature("\\Doctrine\\ORM\\QueryBuilder", "from")
                    .match();

                if (methodMatchParameter == null) {
                    return;
                }

                MethodReference methodReference = methodMatchParameter.getMethodReference();
                String repoName = PhpElementsUtil.getStringValue(methodReference.getParameters()[0]);
                if (repoName != null) {
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

            String underscore = fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(repoName.substring(endIndex + 1));

            // foo_bar => fb
            List<String> starting = new ArrayList<>();
            for (String s : underscore.split("_")) {
                if(!s.isEmpty()) {
                    starting.add(s.substring(0, 1));
                }
            }

            strings.add(StringUtils.join(starting, ""));
            strings.add(underscore);
            strings.add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(strings.iterator().next(), true));

            for(String lookup: strings) {
                completionResultSet.addElement(LookupElementBuilder.create(lookup).withIcon(Symfony2Icons.DOCTRINE));
            }
        }
    }

    private static LookupElementBuilder withModelFieldInfo(@NotNull DoctrineModelField field, @NotNull LookupElementBuilder lookup) {
        lookup = lookup.withTypeText(field.getTypeName(), true);

        if(field.getRelationType() != null) {
            lookup = lookup.withTailText("(" + field.getRelationType() + ")", true);
            lookup = lookup.withTypeText(StringUtils.stripStart(field.getRelation(), "\\"), true);
            lookup = lookup.withIcon(PhpIcons.CLASS);
        } else {
            // relation tail text wins
            String column = field.getColumn();
            if(column != null) {
                lookup = lookup.withTailText("(" + column + ")", true);
            }
        }

        return lookup;
    }

    private void buildLookupElements(CompletionResultSet completionResultSet, QueryBuilderScopeContext collect) {
        for(Map.Entry<String, QueryBuilderPropertyAlias> entry: collect.getPropertyAliasMap().entrySet()) {
            LookupElementBuilder lookup = LookupElementBuilder
                .create(entry.getKey())
                .withIcon(Symfony2Icons.DOCTRINE);

            DoctrineModelField field = entry.getValue().getField();
            if (field != null) {
                lookup = withModelFieldInfo(field, lookup);
            }

            // highlight fields which are possible in select statement
            if(collect.getSelects().contains(entry.getValue().getAlias())) {
                lookup = lookup.withBoldness(true);
            }

            lookup = lookup.withInsertHandler(new DottedClearWorkoutInsertHandler());

            completionResultSet.addElement(lookup);
        }
    }

    public static QueryBuilderMethodReferenceParser getQueryBuilderParser(MethodReference methodReference) {
        final QueryBuilderChainProcessor processor = new QueryBuilderChainProcessor(methodReference);
        processor.collectMethods();

        // @TODO: pipe factory method
        return new QueryBuilderMethodReferenceParser(methodReference.getProject(), new ArrayList<>() {{
            addAll(processor.getQueryBuilderFactoryMethods());
            addAll(processor.getQueryBuilderMethodReferences());
        }});
    }

    /**
     * Workaround to fix duplicated elements after a dot sign
     *
     * "https://blog.jetbrains.com/phpstorm/2023/09/phpstorm-public-roadmap-whats-coming-in-2023-3/#doctrine-query-language-support-inside-querybuilder"
     *
     * Some methods provide language injection from PhpStorm itself, and therefore have supported for dotted prefix ".":
     * - $qb->select('fooBar.id', 'fooBar.');
     * - $qb->select('fooBar.fooBar.id', 'fooBar.id');
     */
    private static class DottedClearWorkoutInsertHandler implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            Editor editor = context.getEditor();
            String insertedText = editor.getDocument().getText(new TextRange(context.getStartOffset(), context.getTailOffset()));
            String lookupString = item.getLookupString();

            int substring = lookupString.indexOf(".");
            if (substring > 0) {
                String beforeDot = lookupString.substring(0, substring);
                TextRange rangeBeforeInserted = new TextRange(context.getStartOffset() - beforeDot.length() - 1, context.getStartOffset());
                String textBeforeDot = editor.getDocument().getText(rangeBeforeInserted);

                // if final inserted lookup string result in duplication remove it
                if (insertedText.startsWith(textBeforeDot)) {
                    context.getDocument().deleteString(rangeBeforeInserted.getStartOffset(), rangeBeforeInserted.getEndOffset());
                    context.commitDocument();
                }
            }
        }
    }
}
