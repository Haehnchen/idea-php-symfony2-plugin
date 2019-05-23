package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpCompletionUtil;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.PhpVariantsUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class PhpConstGotoCompletionProvider extends GotoCompletionProvider {

    private static final String[] SPECIAL_STUB_CONSTANTS = new String[]{"true", "false", "null"};
    private static final String SCOPE_OPERATOR = "::";

    public PhpConstGotoCompletionProvider(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements() {
        PhpIndex phpIndex = PhpIndex.getInstance(this.getProject());
        Collection<LookupElement> elements = new ArrayList<>();

        final String prefix = getElement().getText().replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "");

        // !php/const Foo::<caret>
        if (prefix.contains(SCOPE_OPERATOR)) {
            String classFQN = prefix.substring(0, getElement().getText().indexOf(SCOPE_OPERATOR));
            PhpClass phpClass = PhpElementsUtil.getClass(phpIndex, classFQN);
            if (phpClass != null) {
                // TODO: Not working because we need to reset the prefix matcher
                //   It requires to port this class back to standard CompletionContributor to get more control
                elements.addAll(PhpVariantsUtil.getLookupItems(phpClass.getFields().stream().filter(Field::isConstant).collect(Collectors.toList()), false, null));
            }
            return elements;
        }

        // !php/const FOO
        for (String constantName : phpIndex.getAllConstantNames(null)) {
            if (Arrays.asList(SPECIAL_STUB_CONSTANTS).contains(constantName)) {
                continue;
            }
            elements.addAll(PhpVariantsUtil.getLookupItems(phpIndex.getConstantsByName(constantName), false, null));
        }

        // !php/const Foo
        for (String className : phpIndex.getAllClassNames(null)) {
            for (PhpClass phpClass : phpIndex.getClassesByName(className)) {
                if (hasClassConstantFields(phpClass)) {
                    elements.add(wrapInsertHandler(new MyPhpLookupElement(phpClass)));
                }
            }
            for (PhpClass phpClass : phpIndex.getInterfacesByName(className)) {
                if (hasClassConstantFields(phpClass)) {
                    elements.add(wrapInsertHandler(new MyPhpLookupElement(phpClass)));
                }
            }
        }

        return elements;
    }

    private static boolean hasClassConstantFields(@NotNull PhpClass phpClass) {
        return phpClass.getFields().stream().anyMatch(Field::isConstant);
    }

    private static MyPhpLookupElement wrapInsertHandler(MyPhpLookupElement lookupElement) {
        return lookupElement.withInsertHandler(classInsertHandler);
    }

    private static class MyPhpLookupElement extends PhpLookupElement {

        public MyPhpLookupElement(@NotNull PhpNamedElement namedElement) {
            super(namedElement);
        }

        public MyPhpLookupElement withInsertHandler(InsertHandler insertHandler) {
            this.handler = insertHandler;
            return this;
        }
    }

    private static final PhpClassWithConstantInsertHandler classInsertHandler = new PhpClassWithConstantInsertHandler();

    public static class PhpClassWithConstantInsertHandler extends PhpReferenceTrimBackslashInsertHandler {

        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
            super.handleInsert(context, lookupElement);
            context.getDocument().insertString(context.getTailOffset(), "::");
            context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
            PhpCompletionUtil.showCompletion(context);
        }
    }
}
