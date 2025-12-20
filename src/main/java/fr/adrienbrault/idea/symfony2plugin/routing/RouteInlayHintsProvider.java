package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.RowIcon;
import com.intellij.util.IconUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides inline action hints for Symfony Route attributes, similar to Spring's @RequestMapping actions
 */
@SuppressWarnings("UnstableApiUsage")
public class RouteInlayHintsProvider implements InlayHintsProvider<NoSettings> {
    
    private static final String ROUTE_ATTRIBUTE_FQN = "\\Symfony\\Component\\Routing\\Attribute\\Route";
    private static final SettingsKey<NoSettings> KEY = new SettingsKey<>("symfony.route.actions");

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
        return "Symfony Route Actions";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return """
                class MyController {
                    #[Route('/api/users', name: 'users_list')]
                    public function list() {}
                }
                """;
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
        return new RouteInlayCollector(editor);
    }

    private static class RouteInlayCollector extends FactoryInlayHintsCollector {

        private final Editor editor;

        public RouteInlayCollector(@NotNull Editor editor) {
            super(editor);
            this.editor = editor;
        }

        @Override
        public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
            // Check if plugin is enabled
            if (!Settings.getInstance(element.getProject()).pluginEnabled) {
                return true;
            }

            // Check if route inlay hints are enabled
            if (!Settings.getInstance(element.getProject()).routeInlayHintsEnabled) {
                return true;
            }

            // Skip during indexing
            if (DumbService.isDumb(element.getProject())) {
                return true;
            }

            // Only process PHP attributes
            if (!(element instanceof PhpAttribute attribute)) {
                return true;
            }

            // Check if it's a Route attribute
            String fqn = attribute.getFQN();
            if (fqn == null || !fqn.equals(ROUTE_ATTRIBUTE_FQN)) {
                return true;
            }

            // Get the method this attribute belongs to
            Method method = PsiTreeUtil.getParentOfType(attribute, Method.class);
            if (method == null) {
                return true;
            }

            // Create the inline presentation with actions
            InlayPresentation presentation = createRouteActionsPresentation(attribute, getFactory());

            // Add the inlay hint after the opening parenthesis: #[Route(HERE...
            int offset = findOffsetAfterOpeningParenthesis(attribute);
            if (offset == -1) {
                // If we can't find the opening parenthesis, don't show the hint
                return true;
            }
            sink.addInlineElement(offset, false, presentation, false);

            return true;
        }

        /**
         * Finds the offset right after the opening parenthesis in the attribute.
         * For #[Route('/path')] this returns the position after the '('
         * For multiline attributes like:
         *   #[Route(
         *       '/path'
         *   )]
         * This returns the position before the first parameter (after whitespace)
         */
        private int findOffsetAfterOpeningParenthesis(@NotNull PhpAttribute attribute) {
            String text = attribute.getText();
            int parenIndex = text.indexOf('(');
            if (parenIndex == -1) {
                return -1;
            }

            int baseOffset = attribute.getTextRange().getStartOffset();
            int positionAfterParen = parenIndex + 1;

            // Check if there's a line break after the opening parenthesis
            // If so, skip whitespace and position before the first parameter
            if (positionAfterParen < text.length()) {
                // Find the next non-whitespace character after '('
                int nextNonWhitespace = positionAfterParen;
                while (nextNonWhitespace < text.length() && Character.isWhitespace(text.charAt(nextNonWhitespace))) {
                    nextNonWhitespace++;
                }

                // Check if we found a line break between '(' and the first parameter
                String betweenParenAndParam = text.substring(positionAfterParen, nextNonWhitespace);
                if (betweenParenAndParam.contains("\n") || betweenParenAndParam.contains("\r")) {
                    // Position before the first parameter (after whitespace)
                    return baseOffset + nextNonWhitespace;
                }
            }

            // No line break, position right after '('
            return baseOffset + positionAfterParen;
        }

        @NotNull
        private InlayPresentation createRouteActionsPresentation(@NotNull PhpAttribute attribute, @NotNull PresentationFactory factory) {
            // Create the main action icon (web icon with chevron)
            InlayPresentation actionIcon = factory.smallScaledIcon(createRouteActionIcon());

            // Create clickable presentation that opens action popup
            InlayPresentation clickableAction = factory.referenceOnHover(
                actionIcon,
                (event, translated) -> {
                    // Show action popup
                    RouteActionPopupHandler.showPopup(attribute, event, this.editor);
                }
            );

            // Offset from top for better alignment with text
            return factory.offsetFromTopForSmallText(clickableAction);
        }

        @NotNull
        private Icon createRouteActionIcon() {
            // Create a composite icon with web/globe icon and chevron down
            // Similar to Spring's "Actions for URL" icon
            
            // Use AllIcons.General.Web as the base icon (scaled down to be smaller than text)
            Icon baseIcon = IconUtil.scale(
                AllIcons.General.Web,
                null,
                0.75f // Scale to 75% of original size to make it smaller than text
            );
            
            // Add a chevron / dropdown indicator (also scaled down)
            Icon chevronIcon = IconUtil.scale(
                AllIcons.General.ChevronDown,
                null,
                0.75f
            );
            
            // Create a shifted version of the chevron icon (moved 4px left to remove whitespace)
            Icon shiftedChevron = new Icon() {
                @Override
                public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
                    chevronIcon.paintIcon(c, g, x - 2, y); // Shift 4px to the left
                }
                
                @Override
                public int getIconWidth() {
                    return chevronIcon.getIconWidth() - 2; // Reduce width by 4px
                }
                
                @Override
                public int getIconHeight() {
                    return chevronIcon.getIconHeight();
                }
            };
            
            // Use RowIcon with proper vertical alignment

            return new RowIcon(baseIcon, shiftedChevron);
        }
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return language.isKindOf(PhpLanguage.INSTANCE);
    }
}
