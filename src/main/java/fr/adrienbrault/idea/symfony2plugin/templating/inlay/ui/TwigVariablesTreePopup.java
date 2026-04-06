package fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.InlayHintsSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.pom.Navigatable;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import fr.adrienbrault.idea.symfony2plugin.templating.inlay.TwigVariableInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariablesTreePopup {
    private static final int POPUP_WIDTH = 392;
    private static final int MIN_VISIBLE_ROWS = 6;
    private static final int MAX_VISIBLE_ROWS = 16;

    public static void show(
        @NotNull TwigFile file,
        @NotNull Editor editor,
        @NotNull Map<String, PsiVariable> variables,
        @NotNull MouseEvent mouseEvent
    ) {
        List<VariableTreeEntry> variableEntries = new ArrayList<>();
        for (Map.Entry<String, PsiVariable> entry : variables.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, PsiVariable>, Integer>comparing(e -> e.getKey().equals("app") ? 1 : 0)
                    .thenComparing(Map.Entry.comparingByKey()))
                .toList()) {
            String varName = entry.getKey();
            PsiVariable psiVariable = entry.getValue();

            Set<String> resolvedTypes = resolveTypes(file.getProject(), psiVariable.getTypes());
            TwigVariableTreeNode varData = new TwigVariableTreeNode(varName, resolvedTypes, null);
            List<TwigVariableTreeNode> properties = TwigVariableTreeBuilder.buildProperties(file.getProject(), varName, resolvedTypes);
            variableEntries.add(new VariableTreeEntry(varData, properties));
        }
        Set<String> listVariables = new HashSet<>();
        Map<String, Set<String>> variableTypes = new HashMap<>();
        for (VariableTreeEntry entry : variableEntries) {
            variableTypes.put(entry.variableNode.name(), entry.variableNode.types());
            if (entry.variableNode.isList()) {
                listVariables.add(entry.variableNode.name());
            }
        }

        DefaultTreeModel model = new DefaultTreeModel(buildFilteredRoot(variableEntries, ""));
        Tree tree = new Tree(model) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferred = super.getPreferredSize();
                Rectangle bounds = getLastVisibleRowBounds(this, getRowCount());
                if (bounds == null) {
                    return preferred;
                }

                Insets insets = getInsets();
                int contentHeight = bounds.y + bounds.height + insets.bottom;
                return new Dimension(preferred.width, contentHeight);
            }
        };
        tree.setRootVisible(false);
        tree.setCellRenderer(new TwigVariableTreeActionCellRenderer());

        TwigVariableInsertHandler insertHandler = new TwigVariableInsertHandler(editor, file);

        com.intellij.openapi.ui.popup.JBPopup[] popupRef = new com.intellij.openapi.ui.popup.JBPopup[1];

        SearchTextField searchField = new SearchTextField();
        searchField.setToolTipText("Filter variables");
        Font editorFont = searchField.getTextEditor().getFont();
        searchField.getTextEditor().setFont(editorFont.deriveFont(Math.max(9f, editorFont.getSize2D() - 2f)));

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                if (path == null) return;

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (!(node.getUserObject() instanceof TwigVariableTreeNode data)) return;

                int depth = path.getPathCount(); // root(1) + variable(2) + property(3)
                Rectangle rowBounds = tree.getPathBounds(path);
                if (rowBounds == null) return;
                if (e.getY() < rowBounds.y || e.getY() > rowBounds.y + rowBounds.height) {
                    return;
                }

                // Keep expand/collapse controls clickable without triggering row actions.
                if (e.getX() < rowBounds.x) {
                    return;
                }

                if (isInsertIconHit(tree, e, rowBounds)) {
                    if (depth == 2) {
                        if (data.isList()) {
                            insertHandler.insertEmptyForeach(data.name());
                        } else if (data.isBool()) {
                            insertHandler.insertIfStatement(data.name());
                        } else {
                            insertHandler.insertPrintBlock(data.name());
                        }
                    } else if (depth == 3) {
                        String parentVar = data.parentVariable();
                        if (parentVar != null && listVariables.contains(parentVar)) {
                            insertHandler.insertForeachPropertyAccess(parentVar, data.name());
                        } else if (data.isList() && parentVar != null) {
                            insertHandler.insertFilledForeach(parentVar, data.name());
                        } else {
                            String expr = (parentVar != null ? parentVar + "." : "") + data.name();
                            insertHandler.insertPrintBlock(expr);
                        }
                    }

                    if (popupRef[0] != null) {
                        popupRef[0].cancel();
                    }
                    return;
                }

                Navigatable target = ReadAction.compute(() -> findNavigationTarget(file.getProject(), data, variableTypes));
                if (target != null) {
                    target.navigate(true);
                    if (popupRef[0] != null) {
                        popupRef[0].cancel();
                    }
                }
            }
        });

        tree.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    tree.setCursor(Cursor.getDefaultCursor());
                    return;
                }

                Rectangle rowBounds = tree.getPathBounds(path);
                if (rowBounds == null || e.getY() < rowBounds.y || e.getY() > rowBounds.y + rowBounds.height) {
                    tree.setCursor(Cursor.getDefaultCursor());
                    return;
                }

                if (isInsertIconHit(tree, e, rowBounds)) {
                    tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    tree.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        // Settings button in top row (next to search)
        JLabel settingsLabel = new JLabel(AllIcons.General.GearPlain);
        settingsLabel.setBorder(JBUI.Borders.empty(2, 6));
        settingsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsLabel.setToolTipText("Options");
        settingsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();

                JMenuItem disableItem = new JMenuItem("Disable inlay hints");
                disableItem.addActionListener(ev -> {
                    Project project = file.getProject();
                    InlayHintsSettings.instance().changeHintTypeStatus(new SettingsKey<>("symfony.twig.root.variables"), TwigLanguage.INSTANCE, false);
                    DaemonCodeAnalyzer.getInstance(project).restart(file);

                    Notification notification = NotificationGroupManager.getInstance()
                        .getNotificationGroup("Symfony Notifications")
                        .createNotification(
                            "Twig Variable Inlay Hints Disabled",
                            "Twig variable inlay hints were disabled. Re-enable them in Settings > Editor > Inlay Hints > Other > Twig > Twig Root Variables.",
                            NotificationType.INFORMATION
                        );
                    notification.notify(project);

                    if (popupRef[0] != null) popupRef[0].cancel();
                });
                menu.add(disableItem);
                menu.show(settingsLabel, 0, settingsLabel.getHeight());
            }
        });

        JPanel content = new JPanel(new BorderLayout());
        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        scrollPane.setPreferredSize(new Dimension(POPUP_WIDTH, getTreeViewportHeight(tree)));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(UIUtil.getPanelBackground());
        header.setBorder(JBUI.Borders.empty(4, 6, 2, 6));
        header.add(searchField, BorderLayout.CENTER);
        header.add(settingsLabel, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);

        popupRef[0] = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, tree)
            .setRequestFocus(true)
            .setResizable(true)
            .createPopup();

        Runnable updatePopupSize = () -> SwingUtilities.invokeLater(() -> {
            if (popupRef[0] == null) {
                return;
            }

            int viewportHeight = getTreeViewportHeight(tree);
            scrollPane.setPreferredSize(new Dimension(POPUP_WIDTH, viewportHeight));

            JComponent popupContent = popupRef[0].getContent();

            popupContent.revalidate();
            int chromeHeight = popupRef[0].getSize().height - popupContent.getHeight();
            int targetHeight = viewportHeight + header.getPreferredSize().height + chromeHeight;
            popupRef[0].setSize(new Dimension(POPUP_WIDTH, targetHeight));
        });

        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                updatePopupSize.run();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                updatePopupSize.run();
            }
        });

        searchField.getTextEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }

            private void applyFilter() {
                String query = searchField.getText().trim();
                DefaultMutableTreeNode filteredRoot = buildFilteredRoot(variableEntries, query);
                model.setRoot(filteredRoot);
                model.reload();
                if (!query.isEmpty()) {
                    expandAll(tree);
                }
                updatePopupSize.run();
            }
        });

        popupRef[0].show(new RelativePoint(mouseEvent));

        SwingUtilities.invokeLater(() -> {
            if (popupRef[0] == null) {
                return;
            }

            updatePopupSize.run();
            searchField.requestFocusInWindow();
        });
    }

    private static void expandAll(@NotNull JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    @NotNull
    private static DefaultMutableTreeNode buildFilteredRoot(@NotNull List<VariableTreeEntry> variableEntries, @NotNull String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        boolean hasQuery = !normalizedQuery.isBlank();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Variables");

        for (VariableTreeEntry variableEntry : variableEntries) {
            TwigVariableTreeNode variableNodeData = variableEntry.variableNode;
            boolean variableMatches = !hasQuery || matchesQuery(variableNodeData, normalizedQuery);

            List<TwigVariableTreeNode> filteredProperties = new ArrayList<>();
            for (TwigVariableTreeNode propertyNodeData : variableEntry.propertyNodes) {
                if (!hasQuery || variableMatches || matchesQuery(propertyNodeData, normalizedQuery)) {
                    filteredProperties.add(propertyNodeData);
                }
            }

            if (!variableMatches && filteredProperties.isEmpty()) {
                continue;
            }

            DefaultMutableTreeNode variableNode = new DefaultMutableTreeNode(variableNodeData);
            for (TwigVariableTreeNode propertyNodeData : filteredProperties) {
                variableNode.add(new DefaultMutableTreeNode(propertyNodeData, false));
            }

            root.add(variableNode);
        }

        return root;
    }

    private static boolean matchesQuery(@NotNull TwigVariableTreeNode node, @NotNull String query) {
        if (query.isBlank()) {
            return true;
        }

        String name = node.name().toLowerCase(Locale.ROOT);
        if (name.contains(query)) {
            return true;
        }

        String type = node.getTypeDisplay().toLowerCase(Locale.ROOT);
        if (type.contains(query)) {
            return true;
        }

        String parent = node.parentVariable();
        return parent != null && (parent + "." + node.name()).toLowerCase(Locale.ROOT).contains(query);
    }

    private static boolean isInsertIconHit(@NotNull JTree tree, @NotNull MouseEvent e, @NotNull Rectangle rowBounds) {
        Rectangle visible = tree.getVisibleRect();
        int actionZoneWidth = TwigVariableTreeActionCellRenderer.ACTION_ICON.getIconWidth() + TwigVariableTreeActionCellRenderer.ACTION_ICON_RIGHT_INSET + 14;
        int zoneRight = visible.x + visible.width - 4;
        int zoneLeft = zoneRight - actionZoneWidth;

        return e.getX() >= zoneLeft && e.getX() <= zoneRight && e.getY() >= rowBounds.y && e.getY() <= rowBounds.y + rowBounds.height;
    }

    private static Navigatable findNavigationTarget(
        @NotNull Project project,
        @NotNull TwigVariableTreeNode node,
        @NotNull Map<String, Set<String>> variableTypes
    ) {
        if (!node.isPropertyNode()) {
            return findClassNavigatable(project, node.types());
        }

        String parentVariable = node.parentVariable();
        if (parentVariable == null) {
            return null;
        }

        Set<String> parentTypes = variableTypes.get(parentVariable);
        if (parentTypes == null) {
            return null;
        }

        for (String type : parentTypes) {
            String baseType = type.replaceAll("\\[]$", "");
            if (baseType.isBlank() || !baseType.startsWith("\\")) {
                continue;
            }

            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, baseType);
            if (phpClass == null) {
                continue;
            }

            for (Method method : phpClass.getMethods()) {
                if (!TwigTypeResolveUtil.isTwigAccessibleMethod(method)) {
                    continue;
                }

                String propName = TwigTypeResolveUtil.getPropertyShortcutMethodName(method);
                if (node.name().equals(propName)) {
                    return method;
                }
            }

            for (Field field : phpClass.getFields()) {
                if (!field.getModifier().isPublic() || field.getModifier().isStatic()) {
                    continue;
                }

                if (node.name().equals(field.getName())) {
                    return field;
                }
            }
        }

        return null;
    }

    private static Navigatable findClassNavigatable(@NotNull Project project, @NotNull Set<String> types) {
        for (String type : types) {
            String baseType = type.replaceAll("\\[\\]$", "");
            if (baseType.isBlank() || !baseType.startsWith("\\")) {
                continue;
            }

            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, baseType);
            if (phpClass != null) {
                return phpClass;
            }
        }

        return null;
    }

    private static int getTreeViewportHeight(@NotNull JTree tree) {
        int rowCount = Math.max(tree.getRowCount(), 0);
        int visibleRows = Math.min(TwigVariablesTreePopup.MAX_VISIBLE_ROWS, Math.max(TwigVariablesTreePopup.MIN_VISIBLE_ROWS, rowCount));
        Rectangle lastVisibleRowBounds = getLastVisibleRowBounds(tree, visibleRows);
        if (lastVisibleRowBounds != null) {
            // Keep a tiny guard pixel budget to avoid borderline "empty-space only" vertical scrollbars.
            return lastVisibleRowBounds.y + lastVisibleRowBounds.height + 2;
        }

        Insets insets = tree.getInsets();
        int rowHeight = getEffectiveRowHeight(tree);
        return insets.top + (visibleRows * rowHeight) + insets.bottom + 2;
    }

    private static Rectangle getLastVisibleRowBounds(@NotNull JTree tree, int visibleRows) {
        if (visibleRows <= 0) {
            return null;
        }
        return tree.getRowBounds(visibleRows - 1);
    }

    private static int getEffectiveRowHeight(@NotNull JTree tree) {
        int rowHeight = tree.getRowHeight();
        if (rowHeight > 0) {
            return rowHeight;
        }

        Rectangle firstRow = tree.getRowBounds(0);
        if (firstRow != null && firstRow.height > 0) {
            return firstRow.height;
        }

        return 20;
    }

    private static Set<String> resolveTypes(@NotNull Project project, @NotNull Set<String> rawTypes) {
        PhpType resolved = PhpIndex.getInstance(project).completeType(
            project, PhpType.from(rawTypes.toArray(new String[0])), new HashSet<>()
        );
        Set<String> result = new LinkedHashSet<>();
        for (String t : resolved.getTypes()) {
            if (!t.isBlank() && !PhpType.isUnresolved(t)) {
                result.add(t);
            }
        }
        return result.isEmpty() ? rawTypes : result;
    }

    private record VariableTreeEntry(@NotNull TwigVariableTreeNode variableNode, @NotNull List<TwigVariableTreeNode> propertyNodes) {
    }
}
