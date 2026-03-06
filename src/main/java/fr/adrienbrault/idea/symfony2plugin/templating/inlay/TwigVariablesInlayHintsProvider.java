package fr.adrienbrault.idea.symfony2plugin.templating.inlay;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.SequencePresentation;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui.TwigVariablesTreePopup;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * Block-level inlay hint for Twig templates rendered by a controller.
 * Clicking opens a tree popup for code insertion and navigation.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@SuppressWarnings("UnstableApiUsage")
public class TwigVariablesInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    private static final SettingsKey<NoSettings> KEY = new SettingsKey<>("symfony.twig.root.variables");

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public String getName() {
        return "Twig Root Variables";
    }

    @Override
    public String getPreviewText() {
        return null;
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return changeListener -> new JPanel();
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @NotNull
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file, @NotNull Editor editor, @NotNull NoSettings settings, @NotNull InlayHintsSink sink) {
        return new TwigVariablesInlayCollector(editor);
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return language.isKindOf(TwigLanguage.INSTANCE);
    }

    private static class TwigVariablesInlayCollector extends FactoryInlayHintsCollector {

        TwigVariablesInlayCollector(@NotNull Editor editor) {
            super(editor);
        }

        @Override
        public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
            if (!(element instanceof TwigFile twigFile)) {
                return true;
            }

            if (DumbService.isDumb(element.getProject())) {
                return false;
            }

            if (!Symfony2ProjectComponent.isEnabled(element)) {
                return false;
            }

            if (TwigUtil.findTwigFileController(twigFile).isEmpty() && TwigUtil.getTwigFileMethodUsageOnIndex(twigFile).isEmpty()) {
                return false;
            }

            Map<String, PsiVariable> variables = TwigTypeResolveUtil.collectScopeVariables(element);
            if (variables.isEmpty()) {
                return false;
            }

            PresentationFactory factory = getFactory();
            InlayPresentation icon = factory.inset(factory.scaledIcon(Symfony2Icons.SYMFONY, 0.77f), 0, 0, 1, 0);
            InlayPresentation label = factory.smallText(" Variables (" + variables.size() + ")");
            InlayPresentation combined = new SequencePresentation(List.of(icon, label));
            InlayPresentation clickable = factory.referenceOnHover(combined, (event, translated) ->
                TwigVariablesTreePopup.show(twigFile, editor, variables, event)
            );
            InlayPresentation presentation = factory.inset(factory.offsetFromTopForSmallText(clickable), 0, 4, 1, 4);

            sink.addBlockElement(0, false, true, 0, presentation);
            return false;
        }
    }
}
