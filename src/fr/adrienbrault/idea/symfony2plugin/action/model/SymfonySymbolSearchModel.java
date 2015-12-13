package fr.adrienbrault.idea.symfony2plugin.action.model;

import com.intellij.ide.util.gotoByName.ContributorsBasedGotoByModel;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SymfonySymbolSearchModel extends ContributorsBasedGotoByModel {

    public SymfonySymbolSearchModel(@NotNull Project project, @NotNull ChooseByNameContributor[] contributors) {
        super(project, contributors);
    }

    @Override
    public String getPromptText() {
        return "Symfony Symbol";
    }

    @Override
    public String getNotInMessage() {
        return "Nothing found";
    }

    @Override
    public String getNotFoundMessage() {
        return "Nothing found";
    }

    @Nullable
    @Override
    public String getCheckBoxName() {
        return null;
    }

    @Override
    public char getCheckBoxMnemonic() {
        return 0;
    }

    @Override
    public boolean loadInitialCheckBoxState() {
        return false;
    }

    @Override
    public void saveInitialCheckBoxState(boolean state) {

    }

    @NotNull
    @Override
    public String[] getSeparators() {
        return new String[0];
    }

    @Nullable
    @Override
    public String getFullName(Object element) {
        return element.toString();
    }

    @Override
    public boolean willOpenEditor() {
        return true;
    }
}
