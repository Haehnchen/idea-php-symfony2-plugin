package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.FieldReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.FormUnderscoreMethodReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpIncompleteCompletionContributor extends CompletionContributor {
    public PhpIncompleteCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                PsiElement parent;
                PsiElement prevSibling = completionParameters.getPosition().getPrevSibling();
                if (prevSibling != null && prevSibling.getNode().getElementType() == PhpTokenTypes.ARROW) {
                    // $foo->
                    parent = prevSibling.getParent();
                } else {
                    // $foo->ad
                    parent = psiElement.getParent();
                }

                if (parent instanceof FieldReference fieldReference) {
                    PhpExpression classReference = fieldReference.getClassReference();
                    if (classReference != null) {
                        Project project = psiElement.getProject();
                        if (PhpIndex.getInstance(project).completeType(project, classReference.getType(), new HashSet<>()).getTypes().stream().anyMatch(s -> s.contains("\\Symfony\\Component\\Form\\FormBuilderInterface"))) {
                            PhpClass phpClass = FormOptionsUtil.getFormPhpClassFromContext(psiElement);
                            if (phpClass == null) {
                                return;
                            }

                            Set<String> alreadyKnownFields = new HashSet<>();
                            String formTypeClassFromScope = FormUtil.getFormTypeClassFromScope(parent);
                            if (formTypeClassFromScope != null) {
                                PhpClass clazz = PhpElementsUtil.getClassInterface(project, formTypeClassFromScope);
                                if (clazz != null) {
                                    FormFieldResolver.visitFormReferencesFields(clazz, twigTypeContainer -> alreadyKnownFields.add(twigTypeContainer.getStringElement()));
                                }
                            }

                            FormUnderscoreMethodReference.visitPropertyPath(
                                phpClass,
                                pair -> completionResultSet.addElement(new MyLookupElement(
                                    pair.getFirst(),
                                    "add",
                                    pair.getSecond(),
                                    phpClass.getName(),
                                    alreadyKnownFields.contains(pair.getFirst())
                                ))
                            );
                        }
                    }
                }
            }
        });
    }

    private class MyInsertHandler implements InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
        public void handleInsert(@NotNull InsertionContext context, @NotNull com.intellij.codeInsight.lookup.LookupElement item) {
            context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
            context.commitDocument();

            if (!(item instanceof MyLookupElement anonymousFunctionMyLookupElement)) {
                return;
            }

            PsiElement elementAt = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
            if (elementAt == null) {
                return;
            }

            Pair<String, Map<String, String>> guessedFormFieldParameters = FormUtil.getGuessedFormFieldParameters(
                PhpIndex.getInstance(context.getProject()),
                context.getProject(), ((MyLookupElement) item).getKey(),
                ((MyLookupElement) item).getPhpNamedElement()
            );

            String content = "";

            String typeClass = guessedFormFieldParameters.getFirst();
            Map<String, String> options = guessedFormFieldParameters.getSecond();

            content += "add('%s'".formatted(anonymousFunctionMyLookupElement.getKey());

            if (typeClass != null) {
                typeClass = PhpElementsUtil.insertUseIfNecessary(elementAt, typeClass);
                content += ", " + typeClass + "::class";
            }

            Set<Map.Entry<String, String>> entries = options.entrySet();
            if (!entries.isEmpty()) {
                content += ", [";

                List<String> opts = new ArrayList<>();
                for (Map.Entry<String, String> entry : entries) {
                    if (entry.getKey().equals("class")) {
                        String classUse = PhpElementsUtil.insertUseIfNecessary(elementAt, entry.getValue());
                        opts.add("'%s' => %s::class".formatted(entry.getKey(), classUse));
                    } else {
                        opts.add("'%s' => %s".formatted(entry.getKey(), entry.getValue()));
                    }
                }

                content += StringUtils.join(opts, ", ");

                content += "]";
            }

            content += ");\n";

            Project project = context.getProject();
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(context.getDocument());

            int caretModel = context.getEditor().getCaretModel().getOffset();

            PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), content);
            CodeStyleManager.getInstance(project).reformatText(context.getFile(), caretModel, caretModel + content.length());
        }
    }

    private class MyLookupElement extends com.intellij.codeInsight.lookup.LookupElement {
        private final String key;
        private final String lookupElement;

        private final PhpNamedElement phpNamedElement;
        private final String typeText;
        private final @NotNull boolean exists;

        public MyLookupElement(@NotNull String key, @NotNull String lookupElement, @NotNull PhpNamedElement phpNamedElement, @NotNull String typeText, boolean exists) {
            this.key = key;
            this.lookupElement = lookupElement;
            this.phpNamedElement = phpNamedElement;
            this.typeText = typeText;
            this.exists = exists;
        }

        @Override
        public void renderElement(@NotNull LookupElementPresentation presentation) {
            super.renderElement(presentation);

            if (this.exists) {
                presentation.setTypeText(typeText, Symfony2Icons.SYMFONY_AI_OPACITY);
                presentation.setTypeGrayed(true);
            } else {
                presentation.setTypeText(typeText, Symfony2Icons.SYMFONY_AI);
            }

            presentation.setIcon(phpNamedElement.getIcon());
            presentation.setTypeIconRightAligned(true);
        }

        @Override
        public @NotNull String getLookupString() {
            return lookupElement + "('" + getKey() + "')";
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context) {
            new MyInsertHandler().handleInsert(context, this);
        }

        public String getKey() {
            return key;
        }

        public PhpNamedElement getPhpNamedElement() {
            return phpNamedElement;
        }
    }
}
