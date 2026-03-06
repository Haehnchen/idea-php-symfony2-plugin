package fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.php.PhpIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariableTreeCellRenderer extends ColoredTreeCellRenderer {

    @Override
    public void customizeCellRenderer(
        @NotNull JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
        if (!(value instanceof DefaultMutableTreeNode treeNode)) return;
        if (!(treeNode.getUserObject() instanceof TwigVariableTreeNode node)) return;

        Icon icon;
        if (node.isList()) {
            icon = AllIcons.Nodes.DataTables;
        } else if (node.isBool()) {
            icon = AllIcons.Nodes.Variable;
        } else if (node.isPropertyNode()) {
            icon = PhpIcons.CLASS_ATTRIBUTE;
        } else {
            icon = PhpIcons.CLASS;
        }
        setIcon(icon);

        append(node.name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        append("  " + node.getTypeDisplay(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
}
