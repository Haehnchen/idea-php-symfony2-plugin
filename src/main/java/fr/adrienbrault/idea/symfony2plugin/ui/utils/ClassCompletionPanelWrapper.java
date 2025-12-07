package fr.adrienbrault.idea.symfony2plugin.ui.utils;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.util.Alarm;
import com.intellij.util.Consumer;
import com.jetbrains.php.completion.PhpCompletionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClassCompletionPanelWrapper {

    @NotNull
    private final Project project;

    @NotNull
    private final JPanel panel;

    @NotNull
    private final Consumer<String> consumer;

    private EditorTextField field;

    private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private boolean myDisposed = false;

    public ClassCompletionPanelWrapper(@NotNull Project project, @NotNull JPanel panel, @NotNull final Consumer<String> consumer) {
        this.project = project;
        this.panel = panel;
        this.consumer = consumer;
        init();
    }

    public String getClassName() {
        return field.getText();
    }

    public void setClassName(@NotNull String clazz) {
        field.setText(clazz);
        consumer.consume(field.getText());
    }

    private void init() {
        this.field = new EditorTextField("", project, com.jetbrains.php.lang.PhpFileType.INSTANCE);

        PhpCompletionUtil.installClassCompletion(this.field, null, getDisposable(), null);

        this.field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent e) {
                String text = field.getText();
                if (StringUtil.isEmpty(text) || StringUtil.endsWith(text, "\\")) {
                    return;
                }

                addUpdateRequest(250, () -> consumer.consume(field.getText()));
            }
        }, getDisposable());

        GridBagConstraints gbConstraints = new GridBagConstraints();
        gbConstraints.fill = 1;
        gbConstraints.weightx = 1.0D;
        gbConstraints.gridx = 1;
        gbConstraints.gridy = 1;

        panel.add(field, gbConstraints);
    }

    private Disposable getDisposable() {
        return () -> {
            myDisposed = true;
            myAlarm.cancelAllRequests();
        };
    }

    private void addUpdateRequest(final int delay, @NotNull final Runnable runnable)
    {
        SwingUtilities.invokeLater(() -> {
            if (myDisposed) {
                return;
            }
            myAlarm.cancelAllRequests();
            myAlarm.addRequest(runnable, delay);
        });
    }

}
