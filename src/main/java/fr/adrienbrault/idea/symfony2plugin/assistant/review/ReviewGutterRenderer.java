package fr.adrienbrault.idea.symfony2plugin.assistant.review;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.markup.ActiveGutterRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.LineMarkerRenderer;
import com.intellij.openapi.editor.markup.LineMarkerRendererEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Paints a "+" (or "×" if a panel is already open) icon in the gutter on mouse hover,
 * toggling the inline comment panel on click.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ReviewGutterRenderer implements LineMarkerRenderer, LineMarkerRendererEx, ActiveGutterRenderer, Disposable {

    private static final int ICON_AREA_WIDTH = 16;

    private final EditorEx editor;
    private final ReviewSessionManager manager;

    private int hoveredLine = -1;
    private boolean iconColumnHovered = false;

    private ReviewGutterRenderer(@NotNull EditorEx editor, @NotNull ReviewSessionManager manager) {
        this.editor = editor;
        this.manager = manager;
    }

    public static void install(
            @NotNull EditorEx editor,
            @NotNull ReviewSessionManager manager,
            @NotNull Disposable parentDisposable
    ) {
        ReviewGutterRenderer renderer = new ReviewGutterRenderer(editor, manager);
        Disposer.register(parentDisposable, renderer);

        // Reserve 16px in the LEFT free painters area (next to standard line markers)
        editor.getGutterComponentEx().reserveLeftFreePaintersAreaWidth(renderer, ICON_AREA_WIDTH);

        // Cover the full document so we get mouse events on every line
        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                null, 0, editor.getDocument().getTextLength(),
                HighlighterLayer.LAST, HighlighterTargetArea.LINES_IN_RANGE
        );
        highlighter.setGreedyToLeft(true);
        highlighter.setGreedyToRight(true);
        highlighter.setLineMarkerRenderer(renderer);
        Disposer.register(renderer, () -> {
            if (highlighter.isValid()) highlighter.dispose();
        });

        editor.addEditorMouseMotionListener(new EditorMouseMotionListener() {
            @Override
            public void mouseMoved(@NotNull EditorMouseEvent e) {
                renderer.hoveredLine = e.getLogicalPosition().line;
                renderer.iconColumnHovered = renderer.isInsideIconColumn(e.getMouseEvent());
                editor.getGutterComponentEx().repaint();
            }
        }, renderer);

        editor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mouseExited(@NotNull EditorMouseEvent e) {
                renderer.hoveredLine = -1;
                renderer.iconColumnHovered = false;
                editor.getGutterComponentEx().repaint();
            }
        }, renderer);
    }

    // -----------------------------------------------------------------------
    // LineMarkerRenderer
    // -----------------------------------------------------------------------

    @Override
    public void paint(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle r) {
        if (hoveredLine < 0) return;

        boolean panelOpen = ReviewInlayController.hasActivePanel(this.editor, hoveredLine);

        Icon icon;
        if (panelOpen) {
            icon = iconColumnHovered ? AllIcons.Actions.CloseHovered : AllIcons.Actions.Close;
        } else {
            icon = iconColumnHovered ? AllIcons.General.InlineAddHover : AllIcons.General.InlineAdd;
        }

        int lineY = editor.logicalPositionToXY(new LogicalPosition(hoveredLine, 0)).y;
        int lineHeight = editor.getLineHeight();

        // r.x is the start of our LEFT free painters area (Position.LEFT guarantees this)
        int iconX = r.x + (ICON_AREA_WIDTH - icon.getIconWidth()) / 2;
        int iconY = lineY + (lineHeight - icon.getIconHeight()) / 2;

        Component observer = editor instanceof EditorEx ex ? ex.getGutterComponentEx() : null;
        icon.paintIcon(observer, g, iconX, iconY);
    }

    // -----------------------------------------------------------------------
    // LineMarkerRendererEx  –  LEFT so that r.x == our reserved area start
    // -----------------------------------------------------------------------

    @Override
    public @NotNull LineMarkerRendererEx.Position getPosition() {
        return LineMarkerRendererEx.Position.LEFT;
    }

    // -----------------------------------------------------------------------
    // ActiveGutterRenderer
    // -----------------------------------------------------------------------

    @Override
    public boolean canDoAction(@NotNull Editor editor, @NotNull MouseEvent e) {
        return hoveredLine >= 0 && isInsideIconColumn(e);
    }

    @Override
    public void doAction(@NotNull Editor editor, @NotNull MouseEvent e) {
        if (hoveredLine >= 0) {
            ReviewInlayController.toggleCommentPanel((EditorEx) editor, hoveredLine, manager);
        }
    }

    @Override
    public @Nullable Rectangle calcBounds(@NotNull Editor editor, int lineNum, @NotNull Rectangle preferredBounds) {
        if (lineNum != hoveredLine) return null;
        return new Rectangle(preferredBounds.x, preferredBounds.y, ICON_AREA_WIDTH, preferredBounds.height);
    }

    // -----------------------------------------------------------------------
    // Disposable
    // -----------------------------------------------------------------------

    @Override
    public void dispose() {
        ReviewInlayController.closeAllPanels(editor);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private boolean isInsideIconColumn(@NotNull MouseEvent e) {
        EditorGutterComponentEx gutter = editor.getGutterComponentEx();
        Component source = e.getComponent();
        Point p = source == gutter ? e.getPoint() : SwingUtilities.convertPoint(source, e.getPoint(), gutter);
        // Left free painters area starts at getLineMarkerFreePaintersAreaOffset()
        int areaStart = gutter.getLineMarkerFreePaintersAreaOffset();
        return p.x >= areaStart && p.x <= areaStart + ICON_AREA_WIDTH;
    }
}
