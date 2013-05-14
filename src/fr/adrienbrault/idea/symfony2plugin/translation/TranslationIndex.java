package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringParser;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TranslationIndex {

    protected static Map<Project, TranslationIndex> instance = new HashMap<Project, TranslationIndex>();

    protected Project project;
    protected boolean isCacheInvalid = false;

    private TranslationStringMap translationStringMap;
    private Long translationStringMapModified;

    public static TranslationIndex getInstance(Project project){

        TranslationIndex projectInstance = instance.get(project);
        if(projectInstance != null) {
          return projectInstance;
        }

        projectInstance = new TranslationIndex(project);
        instance.put(project, projectInstance);

        return projectInstance;

    }

    public TranslationIndex(Project project) {
        this.project = project;
    }


    public TranslationStringMap getTranslationMap() {

        if(this.isCacheValid()) {
            return this.translationStringMap;
        }

        File translationDirectory = this.getTranslationFile();
        if(null == translationDirectory) {
            return new TranslationStringMap();
        }

        this.translationStringMapModified = translationDirectory.lastModified();
        return this.translationStringMap = new TranslationStringParser().parsePathMatcher(translationDirectory.getPath());
    }

    protected boolean isCacheValid() {

        File translationRootPath = this.getTranslationFile();
        if (null == translationRootPath) {
            return this.isCacheInvalid = false;
        }

        Long translationModified = translationRootPath.lastModified();
        return this.isCacheInvalid = translationModified.equals(translationStringMapModified);
    }

    @Nullable
    protected File getContainerFile() {
        Symfony2ProjectComponent symfony2ProjectComponent = this.project.getComponent(Symfony2ProjectComponent.class);
        return symfony2ProjectComponent.getPathToProjectContainer();
    }

    @Nullable
    protected File getTranslationFile() {

        File serviceMapFile = this.getContainerFile();
        if (null == serviceMapFile) {
            return null;
        }

        // root path of translation is our caching indicator
        File translationRootPath = new File(serviceMapFile.getParentFile().getPath() + "/translations");
        if (!translationRootPath.exists()) {
            return null;
        }

        return translationRootPath;
    }


}
