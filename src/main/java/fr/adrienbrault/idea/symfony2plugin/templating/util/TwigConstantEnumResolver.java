package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Shared resolver for Twig constant()/enum()/enum_cases() targets.
 */
public final class TwigConstantEnumResolver {
    private TwigConstantEnumResolver() {
    }

    @NotNull
    public static Collection<PsiElement> getConstantTargets(@NotNull PsiElement psiElement) {
        if (!TwigPattern.getPrintBlockOrTagFunctionPattern("constant").accepts(psiElement)) {
            return Collections.emptyList();
        }

        return getConstantTargetsForArgument(psiElement);
    }

    @NotNull
    public static Collection<PsiElement> getConstantTargetsForArgument(@NotNull PsiElement psiElement) {

        String contents = psiElement.getText();
        if (StringUtils.isBlank(contents)) {
            return Collections.emptyList();
        }

        if (!contents.contains(":")) {
            return new ArrayList<>(PhpIndex.getInstance(psiElement.getProject()).getConstantsByName(contents));
        }

        String[] parts = contents.split("::");
        if (parts.length != 2) {
            return Collections.emptyList();
        }

        PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), parts[0].replace("\\\\", "\\"));
        if (phpClass == null) {
            return Collections.emptyList();
        }

        Collection<PsiElement> targetPsiElements = new ArrayList<>();
        Field field = phpClass.findFieldByName(parts[1], true);
        if (field != null) {
            targetPsiElements.add(field);
        }

        targetPsiElements.addAll(phpClass.getEnumCases().stream().filter(e -> parts[1].equals(e.getName())).toList());

        return targetPsiElements;
    }

    @NotNull
    public static Collection<PhpClass> getEnumTargets(@NotNull PsiElement psiElement) {
        if (!TwigPattern.getPrintBlockOrTagFunctionPattern("enum", "enum_cases").accepts(psiElement)) {
            return Collections.emptyList();
        }

        String contents = psiElement.getText();
        if (StringUtils.isBlank(contents)) {
            return Collections.emptyList();
        }

        PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), contents.replace("\\\\", "\\"));
        if (phpClass == null || !phpClass.isEnum()) {
            return Collections.emptyList();
        }

        return Collections.singletonList(phpClass);
    }
}
