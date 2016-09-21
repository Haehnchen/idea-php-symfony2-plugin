package fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.component.CaretOverlayComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.util.CaretTextOverlayUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CaretTextOverlayListener implements CaretListener {

    private int startDelayMs = 250;

    private final Object lock = new Object();

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> schedule;

    @Override
    public void caretPositionChanged(final CaretEvent caretEvent) {
        synchronized (lock) {
            if(schedule != null) {
                schedule.cancel(true);
                schedule = null;
            }
        }

        final Editor editor = caretEvent.getEditor();
        removeOverlays(editor);

        if(!(editor instanceof EditorEx)) {
            return;
        }

        final Project project = editor.getProject();
        if(project == null || DumbService.getInstance(project).isDumb() || !Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        synchronized (lock) {
            schedule = executor.schedule(() -> {
                ApplicationManager.getApplication().runReadAction(new MyPsiElementRunnable(project, caretEvent, editor));
            }, startDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void caretAdded(CaretEvent caretEvent) {

    }

    @Override
    public void caretRemoved(CaretEvent caretEvent) {

    }

    private void invokeUiComponent(final @NotNull Editor editor, final @NotNull PsiElement psiElement, final @NotNull CaretTextOverlayElement overlayElement) {
        SwingUtilities.invokeLater(() -> {
            CaretOverlayComponent component = new CaretOverlayComponent(editor, overlayElement.getText(), psiElement.getTextOffset(), psiElement.getLanguage());
            editor.getContentComponent().add(component);
            component.setSize(((EditorEx) editor).getScrollPane().getViewport().getViewSize());
        });
    }

    private void removeOverlays(@NotNull Editor editor) {
        for (Component component : editor.getContentComponent().getComponents()) {
            if(component instanceof CaretOverlayComponent) {
                editor.getContentComponent().remove(component);
            }
        }
    }

    synchronized public void clear() {
        synchronized (lock) {
            executor.shutdownNow();
            schedule = null;
        }
    }

    private class MyPsiElementRunnable implements Runnable {
        private final Project project;
        private final CaretEvent caretEvent;
        private final Editor editor;

        public MyPsiElementRunnable(Project project, CaretEvent caretEvent, Editor editor) {
            this.project = project;
            this.caretEvent = caretEvent;
            this.editor = editor;
        }

        @Override
        public void run() {

            Caret caret = caretEvent.getCaret();
            if(caret == null) {
                return;
            }

            if (DumbService.getInstance(project).isDumb() || project.isDisposed()) {
                return;
            }

            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if(psiFile == null) {
                return;
            }

            CaretTextOverlayArguments args = null;

            for (CaretTextOverlay caretTextOverlay : CaretTextOverlayUtil.getExtensions()) {
                if(!caretTextOverlay.accepts(psiFile.getVirtualFile())) {
                    continue;
                }

                if(args == null) {
                    PsiElement element = psiFile.findElementAt(caret.getOffset());
                    if (element == null) {
                        return;
                    }

                    args = new CaretTextOverlayArguments(caretEvent, psiFile, element);
                }

                CaretTextOverlayElement overlay = caretTextOverlay.getOverlay(args);
                if(overlay == null) {
                    continue;
                }

                invokeUiComponent(editor, args.getPsiElement(), overlay);

                return;
            }
        }
    }
}