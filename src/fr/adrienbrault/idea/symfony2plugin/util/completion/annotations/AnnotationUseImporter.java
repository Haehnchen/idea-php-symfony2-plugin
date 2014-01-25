package fr.adrienbrault.idea.symfony2plugin.util.completion.annotations;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.psi.PhpCodeEditUtil;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;

public class AnnotationUseImporter {

    public static void insertUse(InsertionContext context, String fqnAnnotation) {
        PsiElement element = PsiUtilCore.getElementAtOffset(context.getFile(), context.getStartOffset());
        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(element);

        if(null == scopeForUseOperator) {
            return;
        }

        // PhpCodeInsightUtil.canImport:
        // copied from PhpReferenceInsertHandler; throws an error on PhpContractUtil because of "fully qualified names only"
        // but that is catch on phpstorm side already; looks fixed now so use fqn

        if(!fqnAnnotation.startsWith("\\")) {
            fqnAnnotation = "\\" + fqnAnnotation;
        }

        // this looks suitable! :)
        if(PhpCodeInsightUtil.alreadyImported(scopeForUseOperator, fqnAnnotation) == null) {
            PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument());
            PhpCodeEditUtil.insertUseStatement(fqnAnnotation, scopeForUseOperator);
            PsiDocumentManager.getInstance(context.getProject()).doPostponedOperationsAndUnblockDocument(context.getDocument());
        }


    }

}
