package fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariableTreeActionCellRenderer implements TreeCellRenderer {
    public static final Icon ACTION_ICON = AllIcons.General.Add;
    public static final int ACTION_ICON_RIGHT_INSET = 16;

    private final TwigVariableTreeCellRenderer leftRenderer = new TwigVariableTreeCellRenderer();

    @Override
    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
        Component left = leftRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(left.getBackground());
        panel.add(left, BorderLayout.CENTER);

        JLabel action = new JLabel();
        action.setBorder(JBUI.Borders.empty(0, 6, 0, ACTION_ICON_RIGHT_INSET));
        action.setOpaque(false);

        if (value instanceof DefaultMutableTreeNode treeNode && treeNode.getUserObject() instanceof TwigVariableTreeNode) {
            action.setIcon(ACTION_ICON);
            action.setToolTipText("Insert");
        }

        panel.add(action, BorderLayout.EAST);
        return panel;
    }
}
