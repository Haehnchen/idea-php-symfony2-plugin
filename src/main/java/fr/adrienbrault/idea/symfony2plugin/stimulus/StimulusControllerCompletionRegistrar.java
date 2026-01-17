package fr.adrienbrault.idea.symfony2plugin.stimulus;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
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

            Collection<LookupElement> items = new ArrayList<>();

            // For HTML, use normalized names (e.g., "symfony--ux-chartjs--chart")
            for (var controller : StimulusControllerCompletion.getAllControllers(getProject()).values()) {
                items.add(StimulusControllerCompletion.createLookupElement(controller, false));
            }

            return items;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(@NotNull PsiElement element) {
            String controllerName = element.getText().trim();
            if (controllerName.isEmpty()) {
                return new ArrayList<>();
            }

            return getNavigationTargets(getProject(), controllerName);
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

            Collection<LookupElement> items = new ArrayList<>();

            // For Twig, use original names (e.g., "@symfony/ux-chartjs/chart")
            for (var controller : StimulusControllerCompletion.getAllControllers(getProject()).values()) {
                items.add(StimulusControllerCompletion.createLookupElement(controller, true));
            }

            return items;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(@NotNull PsiElement element) {
            String controllerName = element.getText().trim();
            if (controllerName.isEmpty()) {
                return new ArrayList<>();
            }

            // For Twig, the name could be either original (@symfony/ux-chartjs/chart) or normalized (symfony--ux-chartjs--chart)
            // We need to find the matching controller and get its normalized name for lookup
            return getNavigationTargets(getProject(), resolveControllerName(controllerName));
        }

        /**
         * Resolve the controller name to its normalized form for index lookup.
         * Handles both original names (@symfony/ux-chartjs/chart) and normalized names (symfony--ux-chartjs--chart).
         */
        @NotNull
        private String resolveControllerName(@NotNull String controllerName) {
            for (var controller : StimulusControllerCompletion.getAllControllers(getProject()).values()) {
                if (controller.getTwigName().equals(controllerName) || controller.getNormalizedName().equals(controllerName)) {
                    return controller.getNormalizedName();
                }
            }

            // If not found in our map, try direct lookup with the provided name
            return controllerName;
        }
    }

    /**
     * Get navigation targets (PsiFile) for a given controller name.
     * The controller name should be the normalized name as stored in the index.
     *
     * @param normalizedKey The normalized controller name (e.g., "symfony--ux-chartjs--chart")
     * @return Collection of PsiFile targets
     */
    @NotNull
    private static Collection<PsiElement> getNavigationTargets(@NotNull Project project, @NotNull String normalizedKey) {
        Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
            StimulusControllerStubIndex.KEY,
            normalizedKey,
            GlobalSearchScope.allScope(project)
        );

        return new ArrayList<>(PsiElementUtils.convertVirtualFilesToPsiFiles(project, containingFiles));
    }
}
