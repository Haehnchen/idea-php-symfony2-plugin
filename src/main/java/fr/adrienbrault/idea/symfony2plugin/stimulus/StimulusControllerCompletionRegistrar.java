package fr.adrienbrault.idea.symfony2plugin.stimulus;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.xml.XmlFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Completion for Stimulus controllers in HTML and Twig.
 *
 * Supports:
 * - HTML: <div data-controller="|"> </div>
 * - Twig: {{ stimulus_controller('|') }}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StimulusControllerCompletionRegistrar {

    /**
     * HTML: <div data-controller="|"> </div>
     */
    public static class HtmlDataControllerCompletionRegistrar implements GotoCompletionRegistrar {
        @Override
        public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
            registrar.register(
                HtmlDataControllerPattern.getHtmlDataControllerPattern(),
                psiElement -> {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return null;
                    }
                    return new DataController(psiElement);
                }
            );
        }
    }

    /**
     * Twig: {{ stimulus_controller('|') }}
     */
    public static class StimulusTwigCompletionRegistrar implements GotoCompletionRegistrar {
        @Override
        public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
            registrar.register(
                StimulusTwigPattern.getStimulusControllerPattern(),
                psiElement -> {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return null;
                    }
                    return new StimulusTwigFunction(psiElement);
                }
            );
        }
    }

    /**
     * Pattern for HTML data-controller attribute
     */
    private static class HtmlDataControllerPattern {

        /**
         * Pattern for data-controller attribute value
         * Matches: <div data-controller="|"> </div>
         */
        private static PsiElementPattern.Capture<PsiElement> getHtmlDataControllerPattern() {
            return PlatformPatterns
                .psiElement()
                .withParent(
                    XmlPatterns
                        .xmlAttributeValue()
                        .withParent(
                            XmlPatterns
                                .xmlAttribute("data-controller")
                        )
                )
                .inFile(XmlPatterns.psiFile(XmlFile.class));
        }
    }

    /**
     * Pattern for Twig function call: {{ stimulus_controller('|') }}
     */
    private static class StimulusTwigPattern {

        /**
         * Pattern for: {{ stimulus_controller('|') }}
         */
        private static PsiElementPattern.Capture<PsiElement> getStimulusControllerPattern() {
            return PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("stimulus_controller")
                )
                .withLanguage(TwigLanguage.INSTANCE);
        }
    }

    /**
     * HTML attribute value completion for data-controller
     */
    public static class DataController extends GotoCompletionProvider {

        public DataController(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            if (!Symfony2ProjectComponent.isEnabled(getProject())) {
                return new ArrayList<>();
            }

            Collection<String> controllerNames = StimulusControllerCompletion.getAllControllerNames(getProject());
            Collection<LookupElement> items = new ArrayList<>();

            for (String controllerName : controllerNames) {
                items.add(StimulusControllerCompletion.createLookupElement(controllerName));
            }

            return items;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(@NotNull PsiElement element) {
            // TODO: Navigate to the controller file if possible
            return new ArrayList<>();
        }
    }

    /**
     * Twig function completion for stimulus_controller()
     */
    public static class StimulusTwigFunction extends GotoCompletionProvider {

        public StimulusTwigFunction(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            if (!Symfony2ProjectComponent.isEnabled(getProject())) {
                return new ArrayList<>();
            }

            Collection<String> controllerNames = StimulusControllerCompletion.getAllControllerNames(getProject());
            Collection<LookupElement> items = new ArrayList<>();

            for (String controllerName : controllerNames) {
                items.add(StimulusControllerCompletion.createLookupElement(controllerName));
            }

            return items;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(@NotNull PsiElement element) {
            // TODO: Navigate to the controller file if possible
            return new ArrayList<>();
        }
    }
}
