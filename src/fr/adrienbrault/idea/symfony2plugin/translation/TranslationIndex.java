package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationPsiParser;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TranslationIndex {

    protected static Map<Project, TranslationIndex> instance = new HashMap<>();

    protected Project project;

    @Nullable
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


    synchronized public TranslationStringMap getTranslationMap() {

        if(this.translationStringMap != null && this.isCacheValid()) {
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
        if(!translationModified.equals(translationStringMapModified)) {
            return false;
        }

        // @TODO make this more abstract
        // we check for possible file modifications here per translation file
        if(this.translationStringMap != null) {


            File file = new File(translationRootPath.getPath());

            // use cache in any i/o error
            File[] files = file.listFiles();
            if(null == files) {
                return true;
            }

            // directory is empty or not exits, before and after instance
            Map<String, Long> fileNames = this.translationStringMap.getFileNames();
            if(files.length == 0 && fileNames.size() == 0) {
                return true;
            }

            for (File fileEntry : files) {
                if (!fileEntry.isDirectory()) {
                    String fileName = fileEntry.getName();
                    if(fileName.startsWith("catalogue") && fileName.endsWith("php")) {


                        if(!fileNames.containsKey(fileName)) {
                            return false;
                        }

                        if(!fileNames.get(fileName).equals(fileEntry.lastModified())) {
                            return false;
                        }

                    }
                }
            }


        }

        return true;
    }

    @Nullable
    protected File getTranslationRoot() {

        String translationPath = Settings.getInstance(this.project).pathToTranslation;
        if (!FileUtil.isAbsolute(translationPath)) {
            translationPath = project.getBasePath() + "/" + translationPath;
        }

        File file = new File(translationPath);
        if(!file.exists() || !file.isDirectory()) {
            return null;
        }

        return file;
    }


}
