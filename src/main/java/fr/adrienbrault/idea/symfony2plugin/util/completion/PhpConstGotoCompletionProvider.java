package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpBundle;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpCompletionUtil;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.PhpVariantsUtil;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PhpConstGotoCompletionProvider extends GotoCompletionProvider {

    private static final String[] SPECIAL_STUB_CONSTANTS = new String[]{"true", "false", "null"};
    private static final String SCOPE_OPERATOR = "::";

    public PhpConstGotoCompletionProvider(@NotNull PsiElement element) {
        super(element);
    }

    @Override
    public void getLookupElements(@NotNull GotoCompletionProviderLookupArguments arguments) {
        PhpIndex phpIndex = PhpIndex.getInstance(this.getProject());
        CompletionResultSet resultSet = arguments.getResultSet();

        var elementText = getElement().getText();
        var scopeOperatorPos = elementText.indexOf(SCOPE_OPERATOR);
        var cursorPos = elementText.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED);

        // Class constants:  !php/const Foo::<caret>
        if (scopeOperatorPos > -1 && scopeOperatorPos < cursorPos) {
            var prefix = elementText.replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "");
            var classFQN = prefix.substring(0, scopeOperatorPos);
            var phpClass = PhpElementsUtil.getClassInterface(this.getProject(), classFQN);

            if (phpClass != null) {
                // reset the prefix matcher, starting after ::
                resultSet = resultSet.withPrefixMatcher(prefix.substring(prefix.indexOf(SCOPE_OPERATOR) + 2));
                resultSet.addAllElements(PhpVariantsUtil.getLookupItems(phpClass.getFields().stream().filter(Field::isConstant).collect(Collectors.toList()), false, null));
            }
            return;
        }

        Collection<LookupElement> elements = new ArrayList<>();

        // Global constants:  !php/const BAR
        for (String constantName : phpIndex.getAllConstantNames(resultSet.getPrefixMatcher())) {
            if (Arrays.asList(SPECIAL_STUB_CONSTANTS).contains(constantName)) {
                continue;
            }
            elements.addAll(PhpVariantsUtil.getLookupItems(phpIndex.getConstantsByName(constantName), false, null));
        }

        if (arguments.getParameters().getInvocationCount() <= 1) {
            String completionShortcut = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction("CodeCompletion"));
            resultSet.addLookupAdvertisement(PhpBundle.message("completion.press.again.to.see.more.variants", completionShortcut));

            // Classes and interfaces:  !php/const Foo
            for (String className : phpIndex.getAllClassNames(resultSet.getPrefixMatcher())) {
                addAllClasses(elements, phpIndex.getClassesByName(className));
                addAllClasses(elements, phpIndex.getInterfacesByName(className));
            }
        } else {
            // Constants from all classes and interfaces:  !php/const Foo::BAR
            for (String className : phpIndex.getAllClassNames(null)) {
                addAllClassConstants(elements, phpIndex.getClassesByName(className));
                addAllClassConstants(elements, phpIndex.getInterfacesByName(className));
            }
        }

        arguments.addAllElements(elements);
    }

    private void addAllClasses(Collection<LookupElement> elements, Collection<PhpClass> classes) {
        for (PhpClass phpClass : classes) {
            // Filter by classes only with constants (including inherited constants)
            if (PhpElementsUtil.hasClassConstantFields(phpClass)) {
                elements.add(wrapClassInsertHandler(new MyPhpLookupElement(phpClass)));
            }
        }
    }

    private void addAllClassConstants(Collection<LookupElement> elements, Collection<PhpClass> classes) {
        for (PhpClass phpClass : classes) {
            // All class constants
            List<Field> fields = Arrays.stream(phpClass.getOwnFields()).filter(Field::isConstant).collect(Collectors.toList());
            for (PhpNamedElement field : fields) {
                // Foo::BAR
                String lookupString = phpClass.getName() + SCOPE_OPERATOR + field.getName();
                elements.add(wrapClassConstInsertHandler(new MyPhpLookupElement(field, lookupString)));
            }
        }
    }

    private static MyPhpLookupElement wrapClassInsertHandler(MyPhpLookupElement lookupElement) {
        return lookupElement.withInsertHandler(PhpClassWithScopeOperatorInsertHandler.getInstance());
    }

    private static MyPhpLookupElement wrapClassConstInsertHandler(MyPhpLookupElement lookupElement) {
        return lookupElement.withInsertHandler(PhpReferenceTrimBackslashInsertHandler.getInstance());
    }

    private static class MyPhpLookupElement extends PhpLookupElement {

        MyPhpLookupElement(@NotNull PhpNamedElement namedElement) {
            super(namedElement);
        }

        MyPhpLookupElement(@NotNull PhpNamedElement namedElement, @NotNull String lookupString) {
            super(namedElement);
            this.lookupString = lookupString;
        }

        MyPhpLookupElement withInsertHandler(InsertHandler insertHandler) {
            this.handler = insertHandler;
            return this;
        }
    }

    public static class PhpClassWithScopeOperatorInsertHandler extends PhpReferenceTrimBackslashInsertHandler {
        private static final PhpClassWithScopeOperatorInsertHandler instance = new PhpClassWithScopeOperatorInsertHandler();

        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
            super.handleInsert(context, lookupElement);

            if (context.getCompletionChar() == ':') {
                context.setAddCompletionChar(false);
            }

            if (!PhpInsertHandlerUtil.isStringAtCaret(context.getEditor(), SCOPE_OPERATOR)) {
                PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), SCOPE_OPERATOR);
            }

            PhpCompletionUtil.showCompletion(context);
        }

        public static PhpClassWithScopeOperatorInsertHandler getInstance() {
            return instance;
        }
    }
}
