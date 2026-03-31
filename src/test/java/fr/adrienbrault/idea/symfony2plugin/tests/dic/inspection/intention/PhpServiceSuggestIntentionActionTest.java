package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection.intention;

import com.intellij.openapi.command.WriteCommandAction;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention.PhpServiceSuggestIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention.PhpServiceSuggestIntentionAction
 */
public class PhpServiceSuggestIntentionActionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testStartInWriteActionReturnsFalse() {
        var action = new PhpServiceSuggestIntentionAction("\\Args\\Foo",
            myFixture.configureByText(PhpFileType.INSTANCE, "<?php 'foo';").getFirstChild()
        );
        assertFalse(action.startInWriteAction());
    }

    public void testInsertReplacesServiceCallContents() {
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\nnamespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\nreturn service('old_id');"
        );

        StringLiteralExpression literal = findStringLiteral("old_id");
        assertNotNull(literal);

        WriteCommandAction.runWriteCommandAction(getProject(), (Runnable) () ->
            new PhpServiceSuggestIntentionAction.MyInsertCallback(myFixture.getEditor(), literal).insert("new_id")
        );

        assertEquals(
            "<?php\nnamespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\nreturn service('new_id');",
            myFixture.getEditor().getDocument().getText()
        );
    }

    public void testInsertReplacesRefCallContents() {
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\nnamespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\nreturn ref('old_id');"
        );

        StringLiteralExpression literal = findStringLiteral("old_id");
        assertNotNull(literal);

        WriteCommandAction.runWriteCommandAction(getProject(), (Runnable) () ->
            new PhpServiceSuggestIntentionAction.MyInsertCallback(myFixture.getEditor(), literal).insert("new_id")
        );

        assertEquals(
            "<?php\nnamespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\nreturn ref('new_id');",
            myFixture.getEditor().getDocument().getText()
        );
    }

    public void testInsertReplacesRawAtServiceKeepingAtPrefix() {
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n$x = '@old_id';"
        );

        StringLiteralExpression literal = findStringLiteral("@old_id");
        assertNotNull(literal);

        WriteCommandAction.runWriteCommandAction(getProject(), (Runnable) () ->
            new PhpServiceSuggestIntentionAction.MyInsertCallback(myFixture.getEditor(), literal).insert("new_id")
        );

        assertEquals(
            "<?php\n$x = '@new_id';",
            myFixture.getEditor().getDocument().getText()
        );
    }

    public void testInsertReplacesClassConstantWithQuotedString() {
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\nnamespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\nreturn service(\\Args\\Bar::class);"
        );

        ClassConstantReference classConst = findClassConstantReference();
        assertNotNull(classConst);

        WriteCommandAction.runWriteCommandAction(getProject(), (Runnable) () ->
            new PhpServiceSuggestIntentionAction.MyInsertCallback(myFixture.getEditor(), classConst).insert("new_id")
        );

        assertEquals(
            "<?php\nnamespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\nreturn service('new_id');",
            myFixture.getEditor().getDocument().getText()
        );
    }

    private ClassConstantReference findClassConstantReference() {
        ClassConstantReference[] result = {null};
        myFixture.getFile().accept(new com.intellij.psi.PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(com.intellij.psi.PsiElement element) {
                if (element instanceof ClassConstantReference c && "class".equals(c.getName())) {
                    result[0] = c;
                }
                super.visitElement(element);
            }
        });
        return result[0];
    }

    private StringLiteralExpression findStringLiteral(String contents) {
        int[] result = {-1};
        myFixture.getFile().accept(new com.intellij.psi.PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(com.intellij.psi.PsiElement element) {
                if (element instanceof StringLiteralExpression s && s.getContents().equals(contents)) {
                    result[0] = element.getTextRange().getStartOffset();
                }
                super.visitElement(element);
            }
        });

        if (result[0] < 0) return null;

        com.intellij.psi.PsiElement el = myFixture.getFile().findElementAt(result[0]);
        while (el != null && !(el instanceof StringLiteralExpression)) {
            el = el.getParent();
        }
        return (StringLiteralExpression) el;
    }
}
