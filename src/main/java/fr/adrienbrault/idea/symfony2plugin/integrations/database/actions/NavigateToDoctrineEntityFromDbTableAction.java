package fr.adrienbrault.idea.symfony2plugin.integrations.database.actions;

import com.intellij.database.vfs.DatabaseElementVirtualFileImpl;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.util.OpenSourceUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DoctrineEntityTableNameResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NavigateToDoctrineEntityFromDbTableAction extends DumbAwareAction {

    public NavigateToDoctrineEntityFromDbTableAction() {
        super("Go To Related Doctrine Entity", "Navigate to corresponding Doctrine entity for this table", Symfony2Icons.DOCTRINE);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Object virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = project != null
            && virtualFile instanceof DatabaseElementVirtualFileImpl databaseFile
            && databaseFile.getObjectPath() != null;

        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        String tableName = getTableName(e);
        if (tableName == null) {
            return;
        }

        List<Navigatable> navigatables = findEntityNavigatablesByTableName(project, tableName);
        if (navigatables.isEmpty()) {
            return;
        }

        OpenSourceUtil.navigate(true, true, navigatables);
    }

    @Nullable
    private String getTableName(@NotNull AnActionEvent e) {
        Object virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (!(virtualFile instanceof DatabaseElementVirtualFileImpl databaseFile)) {
            return null;
        }

        return databaseFile.getNameWithoutExtension();
    }

    @NotNull
    public static List<Navigatable> findEntityNavigatablesByTableName(@NotNull Project project, @NotNull String tableName) {
        Set<Navigatable> navigatables = new LinkedHashSet<>();
        Collection<DoctrineModel> models = EntityHelper.getModelClasses(project);

        for (DoctrineModel model : models) {
            PhpClass phpClass = model.getPhpClass();
            if (!DoctrineEntityTableNameResolver.isTableMatch(phpClass, tableName)) {
                continue;
            }

            PsiElement navigationElement = phpClass.getNavigationElement();
            if (navigationElement instanceof Navigatable navigatable) {
                navigatables.add(navigatable);
                continue;
            }

            navigatables.add(phpClass);
        }

        return new ArrayList<>(navigatables);
    }
}
