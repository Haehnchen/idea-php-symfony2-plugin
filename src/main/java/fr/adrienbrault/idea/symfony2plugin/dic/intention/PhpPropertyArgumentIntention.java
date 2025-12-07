package fr.adrienbrault.idea.symfony2plugin.dic.intention;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ThrowableRunnable;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.findUsages.PhpGotoTargetRendererProvider;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.completion.ServicePropertyInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import icons.SymfonyIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class PhpPropertyArgumentIntention extends IntentionAndQuickFixAction implements Iconable, HighPriorityAction {
    @Override
    public @IntentionName @NotNull String getName() {
        return "Symfony: Add Property Service";
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return getName();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @Nullable Editor editor, PsiFile file) {
        if (editor == null) {
            return false;
        }

        FieldReference fieldReference = getElement(editor, file);
        if (fieldReference != null) {
            String name = fieldReference.getName();
            if (name != null && name.length() > 2) {
                PhpExpression classReference1 = fieldReference.getClassReference();
                if (classReference1 != null && "this".equals(classReference1.getName())) {
                    PhpClass phpClassScope = PsiTreeUtil.getParentOfType(fieldReference, PhpClass.class);
                    if (phpClassScope != null) {
                        if (phpClassScope.findFieldByName(name, false) == null) {
                            return ServiceUtil.isPhpClassAService(phpClassScope);
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void applyFix(@NotNull Project project, PsiFile file, @Nullable Editor editor) {
        if (editor == null) {
            return;
        }

        FieldReference fieldReference = getElement(editor, file);
        if (fieldReference == null) {
            return;
        }

        String name = fieldReference.getName();
        if (name == null) {
            return;
        }

        String methodName = null;
        PsiElement parent = fieldReference.getParent();
        if (parent instanceof MethodReference) {
            String name1 = ((MethodReference) parent).getName();
            if (StringUtils.isNotBlank(name1)) {
                methodName = name1;
            }
        }

        List<String> injectionService = ServicePropertyInsertUtil.getInjectionService(project, name, methodName)
            .stream()
            .map(s -> StringUtils.stripStart(s, "\\"))
            .toList();

        if (injectionService.size() == 1) {
            buildProperty(project, fieldReference, injectionService.get(0));
            return;
        }

        List<String> phpClasses = injectionService.stream()
            .map(s -> PhpIndex.getInstance(project).getAnyByFQN(s).iterator().next())
            .distinct()
            .map(PhpNamedElement::getFQN)
            .collect(Collectors.toList());

        JBPopupFactory.getInstance().createPopupChooserBuilder(phpClasses)
            .setTitle("Symfony: Property Service Suggestions")
            .setItemChosenCallback(s -> buildProperty(project, fieldReference, s))
            .setRenderer(new DelegatedNamedElementCellRenderer<>(project))
            .createPopup()
            .showInBestPositionFor(editor);
    }

    private static class DelegatedNamedElementCellRenderer<String> implements ListCellRenderer<String> {

        private final Project project;
        private final PhpGotoTargetRendererProvider.PhpNamedElementPsiElementListCellRenderer delegatedRenderer;

        public DelegatedNamedElementCellRenderer(@NotNull Project project) {
            this.project = project;
            this.delegatedRenderer = new PhpGotoTargetRendererProvider.PhpNamedElementPsiElementListCellRenderer(false);
        }

        public Component getListCellRendererComponent(JList<? extends String> jList, String s, int i, boolean b, boolean b1) {
            return this.delegatedRenderer.getListCellRendererComponent(jList, PhpElementsUtil.getClassInterface(project, java.lang.String.valueOf(s)), i, b, b1);
        }
    }

    private static void buildProperty(@NotNull Project project, @NotNull FieldReference fieldReference, @NotNull String classFqn) {
        try {
            PhpClass phpClassScope = PsiTreeUtil.getParentOfType(fieldReference, PhpClass.class);
            if (phpClassScope == null || !ServiceUtil.isPhpClassAService(phpClassScope)) {
                return;
            }

            WriteCommandAction.writeCommandAction(project)
                .withName("Symfony: Add Property Service")
                .run((ThrowableRunnable<Throwable>) () -> ServicePropertyInsertUtil.appendPropertyInjection(phpClassScope, fieldReference.getName(), classFqn));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Icon getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }

    @Nullable
    private static FieldReference getElement(@NotNull Editor editor, @NotNull PsiFile file) {
        CaretModel caretModel = editor.getCaretModel();

        int position = caretModel.getOffset();
        PsiElement elementAt = file.findElementAt(position);
        if (elementAt == null) {
            return null;
        }

        PsiElement parent = elementAt.getParent();
        return parent instanceof FieldReference ? (FieldReference) parent : null;
    }
}
