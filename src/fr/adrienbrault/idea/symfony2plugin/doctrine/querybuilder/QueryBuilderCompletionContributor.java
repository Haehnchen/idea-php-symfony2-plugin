package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util.MatcherUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class QueryBuilderCompletionContributor extends CompletionContributor {

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
                    return;
                }

                QueryBuilderParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
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
                    .withSignature("\\Doctrine\\ORM\\QueryBuilder", "join")
                    .match();

                if(methodMatchParameter == null) {
                    return;
                }

                QueryBuilderParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                if(qb == null) {
                    return;
                }

                QueryBuilderScopeContext collect = qb.collect();
                for(Map.Entry<String, List<QueryBuilderParser.QueryBuilderRelation>> parameter: collect.getRelationMap().entrySet()) {
                    for(QueryBuilderParser.QueryBuilderRelation relation: parameter.getValue()) {
                        completionResultSet.addElement(LookupElementBuilder.create(parameter.getKey() + "." + relation.getFieldName()).withTypeText(relation.getTargetEntity(), true));
                    }
                }

            }

        });


        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();

                MethodMatcher.MethodMatchParameter methodMatchParameter = MatcherUtil.matchPropertyField(psiElement.getContext());
                if(methodMatchParameter == null) {
                    return;
                }

                QueryBuilderParser qb = getQueryBuilderParser(methodMatchParameter.getMethodReference());
                if(qb == null) {
                    return;
                }

                QueryBuilderScopeContext collect = qb.collect();
                for(Map.Entry<String, QueryBuilderParser.QueryBuilderPropertyAlias> entry: collect.getPropertyAliasMap().entrySet()) {
                    PsiElement field = entry.getValue().getPsiTarget();
                    if(field instanceof Field) {
                        LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(entry.getKey());
                        lookupElementBuilder = EntityHelper.attachAnnotationInfoToLookupElement((Field) field, lookupElementBuilder.withIcon(((Field) field).getIcon()));
                        completionResultSet.addElement(lookupElementBuilder);

                    }

                }

            }

        });

    }

    @Nullable
    public static QueryBuilderParser getQueryBuilderParser(PsiElement psiElement) {

        final Collection<MethodReference> psiElements = new ArrayList<MethodReference>();

        // QueryBuilder can have nested MethodReferences; get statement context
        Statement statement = PsiTreeUtil.getParentOfType(psiElement, Statement.class);
        if(statement == null) {
            return null;
        }

        // add all methodrefs in current statement scope
        psiElements.addAll(PsiTreeUtil.collectElementsOfType(statement, MethodReference.class));

        // are we in var scope? "$qb->" and var declaration
        Variable variable = PsiTreeUtil.findChildOfAnyType(statement, Variable.class);
        if(variable == null) {
            return new QueryBuilderParser(psiElement.getProject(), psiElements);
        }

        // resolve non variable declarations so we can search for references
        if(!variable.isDeclaration()) {
            variable = (Variable) variable.resolve();
            if(variable == null) {
                return new QueryBuilderParser(psiElement.getProject(), psiElements);
            }
        }

        // finally find all querybuilder methodreferences on variable scope
        PhpPsiUtil.hasReferencesInSearchScope(PsiTreeUtil.getParentOfType(variable, Method.class).getUseScope(), variable, new CommonProcessors.FindProcessor<PsiReference>() {
            @Override
            protected boolean accept(PsiReference psiReference) {
                PsiElement variable = psiReference.getElement();

                // get scope of variable statement
                if(variable != null) {
                    PsiElement statement = variable.getParent();
                    if(statement != null) {
                        psiElements.addAll(PsiTreeUtil.collectElementsOfType(statement, MethodReference.class));
                    }
                }

                return false;
            }
        });

        return new QueryBuilderParser(psiElement.getProject(), psiElements);
    }


}
