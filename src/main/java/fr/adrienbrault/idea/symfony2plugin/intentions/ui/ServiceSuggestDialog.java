package fr.adrienbrault.idea.symfony2plugin.intentions.ui;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceSuggestDialog {

    public static void create(final @NotNull Editor editor, @NotNull Collection<String> services, final @NotNull Callback callback) {
        final List<String> list = new ArrayList<>(services);

        JBPopupFactory.getInstance().createPopupChooserBuilder(list)
            .setTitle("Symfony: Service Suggestion")
            .setItemChosenCallback(s -> WriteCommandAction.runWriteCommandAction(editor.getProject(), "Service Suggestion Insert", null, () -> callback.insert(s)))
            .createPopup()
            .showInBestPositionFor(editor);
    }

    public interface Callback {
        void insert(@NotNull String selected);
    }
}
