package fr.adrienbrault.idea.symfony2plugin.assistant.review;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * Inline panel rendered between editor lines for entering review comments.
 * <p>
 * After submitting ({@code Ctrl+Enter} or "Start Review") the panel stays open
 * so the user can add further comments; the text area is cleared automatically.
 * The "×" button in the header closes the panel.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ReviewCommentPanel extends JPanel {

    private final JTextArea textArea;

    public ReviewCommentPanel(@NotNull Consumer<String> onSubmit, @NotNull Runnable onClose) {
        setLayout(new BorderLayout(0, 4));
        setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(6, 8)
        ));
        setBackground(UIUtil.getPanelBackground());
        setOpaque(true);

        // ── Header: label + close button ──────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Code Review Comment");
        title.setForeground(JBColor.GRAY);
        title.setFont(JBUI.Fonts.smallFont());
        header.add(title, BorderLayout.WEST);

        // "×" close button – visible immediately so users know the panel can be dismissed
        JButton closeBtn = new JButton(AllIcons.Actions.Close);
        closeBtn.setRolloverIcon(AllIcons.Actions.CloseHovered);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusable(false);
        closeBtn.setToolTipText("Close (Escape)");
        closeBtn.addActionListener(e -> onClose.run());
        header.add(closeBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ── Body: avatar + text field ─────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(8, 0));
        body.setOpaque(false);

        JLabel avatar = new JLabel(AllIcons.General.User);
        avatar.setVerticalAlignment(SwingConstants.TOP);
        body.add(avatar, BorderLayout.WEST);

        JPanel inputArea = new JPanel(new BorderLayout(0, 4));
        inputArea.setOpaque(false);

        textArea = new JTextArea(3, 60);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(JBUI.Borders.empty(4));
        textArea.setFont(UIUtil.getLabelFont());

        // Ctrl+Enter → submit (panel stays open, text clears)
        textArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "submit");
        textArea.getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = textArea.getText().trim();
                if (!text.isEmpty()) {
                    onSubmit.accept(text);
                    // Panel stays open; clearAndFocus() is called by the controller
                }
            }
        });

        // Escape → close panel
        textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        textArea.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose.run();
            }
        });

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(JBUI.Borders.customLine(JBColor.border(), 1));
        inputArea.add(scroll, BorderLayout.CENTER);

        // ── Footer: hint + "Start Review" button ─────────────────────────
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        buttonBar.setOpaque(false);

        JLabel hint = new JLabel("Ctrl+Enter to comment   Enter to add new line");
        hint.setForeground(JBColor.GRAY);
        hint.setFont(JBUI.Fonts.smallFont());
        buttonBar.add(hint);

        JButton startReview = new JButton("Start Review");
        startReview.addActionListener(e -> {
            String text = textArea.getText().trim();
            if (!text.isEmpty()) {
                onSubmit.accept(text);
            }
        });
        buttonBar.add(startReview);

        inputArea.add(buttonBar, BorderLayout.SOUTH);
        body.add(inputArea, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    /** Clears the text area and returns focus – called after a comment is saved. */
    public void clearAndFocus() {
        textArea.setText("");
        SwingUtilities.invokeLater(this::requestFocusInTextField);
    }

    public void requestFocusInTextField() {
        textArea.requestFocusInWindow();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 110);
        return d;
    }
}
