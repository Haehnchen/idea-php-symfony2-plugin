package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtensionLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Provides completion and navigation for Twig guard tag
 * {% guard function importmap %}
 * {% guard filter upper %}
 * {% guard test even %}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GuardGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        // {% guard <caret> %}
        // Completion for "function", "filter", "test" keywords
        registrar.register(TwigPattern.getGuardTypePattern(), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new GuardTypeGotoCompletionProvider(psiElement);
        });

        // {% guard function <caret> %}
        // Completion for callable name based on type
        registrar.register(TwigPattern.getGuardCallablePattern(), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new GuardCallableGotoCompletionProvider(psiElement);
        });
    }

    /**
     * {% guard <caret> %}
     * Provides completion for "function", "filter", "test" keywords
     */
    private static class GuardTypeGotoCompletionProvider extends GotoCompletionProvider {
        GuardTypeGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            lookupElements.add(LookupElementBuilder.create("function").withTypeText("Twig function", true).withIcon(Symfony2Icons.SYMFONY));
            lookupElements.add(LookupElementBuilder.create("filter").withTypeText("Twig filter", true).withIcon(Symfony2Icons.SYMFONY));
            lookupElements.add(LookupElementBuilder.create("test").withTypeText("Twig test", true).withIcon(Symfony2Icons.SYMFONY));

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            return Collections.emptyList();
        }
    }

    /**
     * {% guard function <caret> %}
     * {% guard filter <caret> %}
     * {% guard test <caret> %}
     *
     * Provides completion and navigation for callable names based on the guard type
     */
    private static class GuardCallableGotoCompletionProvider extends GotoCompletionProvider {
        GuardCallableGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            Project project = getProject();
            String guardType = getGuardType(getElement());

            if ("function".equals(guardType)) {
                // Provide Twig functions
                for (Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getFunctions(project).entrySet()) {
                    lookupElements.add(new TwigExtensionLookupElement(project, entry.getKey(), entry.getValue()));
                }
            } else if ("filter".equals(guardType)) {
                // Provide Twig filters
                for (Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getFilters(project).entrySet()) {
                    lookupElements.add(new TwigExtensionLookupElement(project, entry.getKey(), entry.getValue()));
                }
            } else if ("test".equals(guardType)) {
                // Provide Twig tests
                for (Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getSimpleTest(project).entrySet()) {
                    lookupElements.add(new TwigExtensionLookupElement(project, entry.getKey(), entry.getValue()));
                }
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String text = element.getText();
            if (StringUtils.isBlank(text)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> targets = new ArrayList<>();
            Project project = getProject();
            String guardType = getGuardType(element);

            Map<String, TwigExtension> extensions = null;
            if ("function".equals(guardType)) {
                extensions = TwigExtensionParser.getFunctions(project);
            } else if ("filter".equals(guardType)) {
                extensions = TwigExtensionParser.getFilters(project);
            } else if ("test".equals(guardType)) {
                extensions = TwigExtensionParser.getSimpleTest(project);
            }

            if (extensions != null) {
                for (Map.Entry<String, TwigExtension> entry : extensions.entrySet()) {
                    if (text.equals(entry.getKey())) {
                        ContainerUtil.addIfNotNull(
                            targets,
                            TwigExtensionParser.getExtensionTarget(project, entry.getValue())
                        );
                    }
                }
            }

            return targets;
        }

        /**
         * Get the guard type (function, filter, test) by looking at the previous IDENTIFIER
         * {% guard function importmap %}
         *              ^-------^
         */
        @Nullable
        private String getGuardType(@NotNull PsiElement element) {
            PsiElement prevIdentifier = PsiElementUtils.getPrevSiblingOfType(
                element,
                com.intellij.patterns.PlatformPatterns.psiElement(com.jetbrains.twig.TwigTokenTypes.IDENTIFIER)
            );

            if (prevIdentifier != null) {
                String text = prevIdentifier.getText();
                if ("function".equals(text) || "filter".equals(text) || "test".equals(text)) {
                    return text;
                }
            }

            return null;
        }
    }
}
