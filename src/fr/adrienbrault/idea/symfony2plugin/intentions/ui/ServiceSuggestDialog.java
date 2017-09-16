package fr.adrienbrault.idea.symfony2plugin.intentions.ui;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceSuggestDialog {

    public static void create(final @NotNull Editor editor, @NotNull Collection<String> services, final @NotNull Callback callback) {
        final JBList<String> list = new JBList<>(services);

        JBPopupFactory.getInstance().createListPopupBuilder(list)
            .setTitle("Symfony: Service Suggestion")
            .setItemChoosenCallback(() -> new WriteCommandAction.Simple(editor.getProject(), "Service Suggestion Insert") {
                @Override
                protected void run() {
                    callback.insert((String) list.getSelectedValue());
                }
            }.execute())
            .createPopup()
            .showInBestPositionFor(editor);
    }

    public interface Callback {
        void insert(@NotNull String selected);
    }
}
