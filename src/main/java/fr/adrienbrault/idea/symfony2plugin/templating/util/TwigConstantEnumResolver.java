package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Constant;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Shared resolver for Twig constant()/enum()/enum_cases() targets.
 * Examples: <code>constant('Foo\\Bar::BAZ')</code>, <code>constant('BAZ', typedObject)</code>, <code>constant('App\\BAZ')</code>.
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

        // constant('Foo\\Bar::BAZ') -> class constant / enum case
        // constant('BAZ', object) -> object-relative class constant
        // constant('App\\BAZ') -> global PHP constant
        if (!contents.contains(":")) {
            Collection<PsiElement> objectRelativeTargets = getObjectRelativeConstantTargets(psiElement, contents);
            if (!objectRelativeTargets.isEmpty()) {
                return objectRelativeTargets;
            }

            return getGlobalConstantTargets(psiElement.getProject(), contents);
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

    /**
     * Resolves the object argument; example: <code>constant('CLUBS', order.suite)</code>.
     */
    @NotNull
    public static Collection<PhpClass> getObjectRelativeConstantClasses(@NotNull PsiElement firstArgumentElement) {
        PsiElement expressionElement = TwigUtil.findTwigFunctionSecondArgumentPathLeaf(firstArgumentElement);
        if (expressionElement == null) {
            return Collections.emptyList();
        }

        Collection<String> typePath = TwigTypeResolveUtil.formatPsiTypeNameWithCurrent(expressionElement);
        if (typePath.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<PhpClass> phpClasses = new ArrayList<>();
        Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(expressionElement, typePath);
        for (TwigTypeContainer twigTypeContainer : types) {
            for (PhpClass phpClass : TwigTypeResolveUtil.resolveTwigTypeClasses(expressionElement.getProject(), twigTypeContainer)) {
                if (!TwigTypeResolveUtil.isWeakCollectionLikeClass(phpClass)) {
                    phpClasses.add(phpClass);
                }
            }
        }

        return phpClasses;
    }

    @NotNull
    private static Collection<PsiElement> getObjectRelativeConstantTargets(@NotNull PsiElement psiElement, @NotNull String contents) {
        if (contents.contains("\\") || contents.contains(":")) {
            return Collections.emptyList();
        }

        Collection<PsiElement> targetPsiElements = new ArrayList<>();
        for (PhpClass phpClass : getObjectRelativeConstantClasses(psiElement)) {
            Field field = phpClass.findFieldByName(contents, true);
            if (field != null && field.isConstant() && field.getModifier().isPublic()) {
                targetPsiElements.add(field);
            }
        }

        return targetPsiElements;
    }

    /**
     * Resolves namespaced global constants; example: <code>constant('BugDemo\\NAMESPACED_CONST')</code>.
     */
    @NotNull
    private static Collection<PsiElement> getGlobalConstantTargets(@NotNull Project project, @NotNull String contents) {
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        String fqn = normalizeGlobalConstantFqn(contents);

        Collection<Constant> constants = phpIndex.getConstantsByFQN(fqn);
        if (!constants.isEmpty()) {
            return new ArrayList<>(constants);
        }

        String normalizedName = StringUtils.stripStart(fqn, "\\");
        if (!normalizedName.contains("\\")) {
            return new ArrayList<>(phpIndex.getConstantsByName(normalizedName));
        }

        return Collections.emptyList();
    }

    @NotNull
    private static String normalizeGlobalConstantFqn(@NotNull String contents) {
        return "\\" + StringUtils.stripStart(contents.replace("\\\\", "\\"), "\\");
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
