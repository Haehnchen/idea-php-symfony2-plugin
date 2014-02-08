package fr.adrienbrault.idea.symfony2plugin.profiler;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ProfilerUtil {

    @Nullable
    public static File findProfilerCsv(Project project) {

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);
        for(File file: symfony2ProjectComponent.getContainerFiles()) {
            if(file.exists()) {
                File translationRootPath = new File(file.getParentFile().getPath() + "/profiler/index.csv");
                if (translationRootPath.exists()) {
                    return translationRootPath;
                }
            }
        }

        return null;
    }

}
