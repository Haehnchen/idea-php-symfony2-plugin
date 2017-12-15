package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.navigation.controller.TemplatesControllerRelatedGotoCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Provides template name completion for PHP related calls which will render the given template
 *
 * render('foo.html.twig', ['fo<caret>obar' => 'foobar'])
 *
 * return ['fo<caret>obar' => 'foobar']
 * return array_merge(['fo<caret>obar' => 'foobar'])
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RenderParameterGotoCompletionRegistrar implements GotoCompletionRegistrar {
    /**
     * supported "array merge" function for template variables
     */
    final public static Set<String> ARRAY_FUNCTIONS = new HashSet<>(Arrays.asList(
        "array_merge", "array_merge_recursive", "array_replace"
    ));

    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            PlatformPatterns.psiElement().withParent(StringLiteralExpression.class),
            MyTemplateVariablesGotoCompletionProvider::new
        );
    }

    private static class MyTemplateVariablesGotoCompletionProvider extends GotoCompletionProvider {
        private MyTemplateVariablesGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<String> templateNames = getTemplatesForScope(getElement());
            if(templateNames.size() == 0) {
                return Collections.emptyList();
            }

            Collection<LookupElement> elements = new ArrayList<>();

            for (String templateName : templateNames) {
                for (PsiFile psiFile : TwigHelper.getTemplatePsiElements(getProject(), templateName)) {
                    if(!(psiFile instanceof TwigFile)) {
                        continue;
                    }

                    Map<TwigFile, String> twigFiles = new HashMap<>();

                    twigFiles.put((TwigFile) psiFile, templateName);
                    twigFiles.putAll(TwigUtil.getExtendsTemplates((TwigFile) psiFile));

                    for (Map.Entry<TwigFile, String> entry : twigFiles.entrySet()) {
                        TwigUtil.visitTemplateVariables(entry.getKey(), pair ->
                            elements.add(LookupElementBuilder.create(pair.getFirst())
                                .withIcon(TwigIcons.TwigFileIcon)
                                .withTypeText(entry.getValue(), true)
                                .withBoldness(templateNames.contains(entry.getValue())) // highlight self scope
                            ));
                    }
                }
            }

            return elements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            Collection<String> templateNames = getTemplatesForScope(element);
            if(templateNames.size() == 0) {
                return Collections.emptyList();
            }

            String variable = GotoCompletionUtil.getTextValueForElement(element);
            if(org.apache.commons.lang.StringUtils.isBlank(variable)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> elements = new ArrayList<>();

            for (String templateName : templateNames) {
                for (PsiFile psiFile : TwigHelper.getTemplatePsiElements(getProject(), templateName)) {
                    if(!(psiFile instanceof TwigFile)) {
                        continue;
                    }

                    Map<TwigFile, String> twigFiles = new HashMap<>();

                    twigFiles.put((TwigFile) psiFile, templateName);
                    twigFiles.putAll(TwigUtil.getExtendsTemplates((TwigFile) psiFile));

                    for (Map.Entry<TwigFile, String> entry : twigFiles.entrySet()) {
                        TwigUtil.visitTemplateVariables(entry.getKey(), pair -> {
                            if(variable.equals(pair.getFirst())) {
                                elements.add(pair.getSecond());
                            }
                        });
                    }
                }
            }

            return elements;
        }
    }

    /**
     * Extract template names from function, method or annotations
     */
    @NotNull
    public static Collection<String> getTemplatesForScope(@NotNull PsiElement psiElement) {
        PsiElement stringLiteral = psiElement.getParent();
        if(!(stringLiteral instanceof StringLiteralExpression)) {
            return Collections.emptyList();
        }

        PhpPsiElement arrayCreationElement = PhpElementsUtil.getCompletableArrayCreationElement(stringLiteral);
        if(arrayCreationElement == null) {
            return Collections.emptyList();
        }


        Set<String> templates = new HashSet<>();

        // array_merge($template, ['<caret>'])
        // array_merge(['<caret>' => 'foo'], $template)
        PsiElement parentArrayCreation = arrayCreationElement.getParent();
        if(parentArrayCreation instanceof ParameterList) {
            PsiElement functionReference = parentArrayCreation.getParent();
            if(functionReference instanceof FunctionReference) {
                String name = ((FunctionReference) functionReference).getName();

                if(ARRAY_FUNCTIONS.contains(name)) {
                    arrayCreationElement = (PhpPsiElement) functionReference;
                }
            }
        }

        if(parentArrayCreation instanceof PhpReturn) {
            // fooAction() {
            //   return ['<caret>'];
            // }
            Method method = PsiTreeUtil.getParentOfType(parentArrayCreation, Method.class);
            if(method != null) {
                TemplatesControllerRelatedGotoCollector.visitMethodTemplateNames(method, pair -> templates.add(
                    TwigHelper.normalizeTemplateName(pair.getFirst())
                ));
            }
        } else {
            // foobar('foo.html.twig', ['<caret>'])
            PsiElement prevSibling = arrayCreationElement.getPrevPsiSibling();
            if(prevSibling != null) {
                String stringValue = PhpElementsUtil.getStringValue(prevSibling);
                if(stringValue != null && stringValue.toLowerCase().endsWith(".twig")) {
                    templates.add(
                        TwigHelper.normalizeTemplateName(stringValue)
                    );
                }
            }
        }

        return templates;
    }
}
