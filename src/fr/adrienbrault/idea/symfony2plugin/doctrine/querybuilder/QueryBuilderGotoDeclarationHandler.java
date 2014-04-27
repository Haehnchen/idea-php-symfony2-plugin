package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util.MatcherUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryBuilderGotoDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
            return new PsiElement[0];
        }

        List<PsiElement> psiElements = new ArrayList<PsiElement>();

        attachPropertyGoto((StringLiteralExpression) psiElement.getContext(), psiElements);
        attachJoinGoto((StringLiteralExpression) psiElement.getContext(), psiElements);

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    private void attachJoinGoto(StringLiteralExpression psiElement, List<PsiElement> targets) {

        MethodMatcher.MethodMatchParameter methodMatchParameter = MatcherUtil.matchJoin(psiElement);
        if(methodMatchParameter == null) {
            return;
        }

        QueryBuilderParser qb = QueryBuilderCompletionContributor.getQueryBuilderParser(methodMatchParameter.getMethodReference());
        if(qb == null) {
            return;
        }

        String[] joinSplit = StringUtils.split(psiElement.getContents(), ".");
        if(joinSplit.length != 2) {
            return;
        }

        QueryBuilderScopeContext collect = qb.collect();
        if(!collect.getRelationMap().containsKey(joinSplit[0])) {
            return;
        }

        List<QueryBuilderParser.QueryBuilderRelation> relations = collect.getRelationMap().get(joinSplit[0]);
        for(QueryBuilderParser.QueryBuilderRelation relation: relations) {
            if(joinSplit[1].equals(relation.getFieldName()) && relation.getTargetEntity() != null) {
                PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), relation.getTargetEntity());
                if(phpClass != null) {
                    targets.add(phpClass);
                }

            }
        }


    }

    private void attachPropertyGoto(StringLiteralExpression psiElement, List<PsiElement> targets) {

        MethodMatcher.MethodMatchParameter methodMatchParameter = MatcherUtil.matchPropertyField(psiElement);
        if(methodMatchParameter == null) {
            return;
        }

        QueryBuilderParser qb = QueryBuilderCompletionContributor.getQueryBuilderParser(methodMatchParameter.getMethodReference());
        if(qb == null) {
            return;
        }

        QueryBuilderScopeContext collect = qb.collect();
        String propertyContent = psiElement.getContents();

        for(Map.Entry<String, QueryBuilderParser.QueryBuilderPropertyAlias> entry: collect.getPropertyAliasMap().entrySet()) {
            if(entry.getKey().equals(propertyContent)) {
                targets.add(entry.getValue().getPsiTarget());
            }
        }


    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

}
