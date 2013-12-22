package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationPsiParser;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringParser;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TranslationIndex {

    protected static Map<Project, TranslationIndex> instance = new HashMap<Project, TranslationIndex>();

    protected Project project;

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

        File translationDirectory = this.getTranslationRoot();
        if(null == translationDirectory) {
            return new TranslationStringMap();
        }

        Symfony2ProjectComponent.getLogger().info("translations changed: " + translationDirectory.toString());

        this.translationStringMapModified = translationDirectory.lastModified();
        return this.translationStringMap = new TranslationPsiParser(project).parsePathMatcher(translationDirectory.getPath());
    }

    protected boolean isCacheValid() {

        // symfony2 recreates translation file on change, so folder modtime is caching indicator
        File translationRootPath = this.getTranslationRoot();
        if (null == translationRootPath) {
            return false;
        }

        Long translationModified = translationRootPath.lastModified();
        return translationModified.equals(translationStringMapModified);
    }

    @Nullable
    protected File getTranslationRoot() {

        String translationPath = Settings.getInstance(this.project).pathToTranslation;
        if (!FileUtil.isAbsolute(translationPath)) {
            translationPath = project.getBasePath() + "/" + translationPath;
        }

        File file = new File(translationPath);
        if(!file.exists() || !file.isDirectory()) {
            Symfony2ProjectComponent.getLogger().warn("missing translation path: " + file.toString());
            return null;
        }

        return file;
    }


}
