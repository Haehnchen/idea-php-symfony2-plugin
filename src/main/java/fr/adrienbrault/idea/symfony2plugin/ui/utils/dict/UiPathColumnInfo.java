package fr.adrienbrault.idea.symfony2plugin.ui.utils.dict;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.ColumnInfo;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathInterface;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UiPathColumnInfo {

    public static class PathColumn extends ColumnInfo<UiFilePathInterface, String> {

        public PathColumn() {
            super("Path");
        }

        @Nullable
        @Override
        public String valueOf(UiFilePathInterface file) {
            return file.getPath();
        }
    }

    public static class TypeColumn extends ColumnInfo<UiFilePathInterface, String> {

        private Project project;

        public TypeColumn(Project project) {
            super("Info");
            this.project = project;
        }

        @Nullable
        @Override
        public String valueOf(UiFilePathInterface file) {
            return UiSettingsUtil.getPresentableFilePath(project, file).info();
        }
    }

}
