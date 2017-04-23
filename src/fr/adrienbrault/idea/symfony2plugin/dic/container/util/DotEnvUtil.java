package fr.adrienbrault.idea.symfony2plugin.dic.container.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DotEnvUtil {
    private static final Key<CachedValue<Set<String>>> DOT_ENV_VARIABLE_CACHE = new Key<>("DOT_ENV_VARIABLE_CACHE");

    /**
     * case insensitive filenames
     */
    private static final String[] DOCKER_FILES = {
        "Dockerfile", "dockerfile"
    };

    /**
     * Provide targets for "%env(FOOBAR)%"
     *
     * @param parameter %env(FOOBAR)%
     */
    @NotNull
    public static Collection<PsiElement> getEnvironmentVariableTargetsForParameter(@NotNull Project project, @NotNull String parameter) {
        Collection<PsiElement> targets = new ArrayList<>();

        if(parameter.length() > 7 && parameter.startsWith("%env(") && parameter.endsWith(")%")) {
            targets.addAll(
                DotEnvUtil.getEnvironmentVariableTargets(project, parameter.substring(5, parameter.length() - 2))
            );
        }

        return targets;
    }

    @NotNull
    public static Collection<String> getEnvironmentVariables(@NotNull Project project) {
        CachedValue<Set<String>> cache = project.getUserData(DOT_ENV_VARIABLE_CACHE);
        if (cache == null) {
            cache = CachedValuesManager.getManager(project).createCachedValue(() -> {
                Set<String> items = new HashSet<>();

                DotEnvUtil.visitEnvironment(project, pair ->
                    items.add(pair.getFirst())
                );

                return CachedValueProvider.Result.create(items, PsiModificationTracker.MODIFICATION_COUNT);
                }, false
            );
            project.putUserData(DOT_ENV_VARIABLE_CACHE, cache);
        }

        return cache.getValue();
    }

    @NotNull
    public static Collection<PsiElement> getEnvironmentVariableTargets(@NotNull Project project, @NotNull String environmentVariable) {
        Collection<PsiElement> items = new ArrayList<>();

        DotEnvUtil.visitEnvironment(project, pair -> {
            if(environmentVariable.equals(pair.getFirst())) {
                items.add(pair.getSecond());
            }
        });

        return items;
    }

    private static void visitEnvironment(@NotNull Project project, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        for (VirtualFile virtualFile : FilenameIndex.getAllFilesByExt(project, "env", GlobalSearchScope.allScope(project))) {
            Properties variables = new Properties();
            try {
                variables.load(virtualFile.getInputStream());
            } catch (IOException e) {
                continue;
            }

            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if(file == null) {
                continue;
            }

            for (Map.Entry<Object, Object> variable : variables.entrySet()) {
                Object key = variable.getKey();
                if(key instanceof String) {
                    consumer.accept(Pair.create(key.toString(), file));
                }
            }
        }

        for (PsiFile psiFile : FilenameIndex.getFilesByName(project, "docker-compose.yml", GlobalSearchScope.allScope(project))) {
            if(!(psiFile instanceof YAMLFile)) {
                continue;
            }

            for (YAMLKeyValue yamlKeyValue : YAMLUtil.getTopLevelKeys((YAMLFile) psiFile)) {
                if ("services".equals(yamlKeyValue.getKeyText())) {
                    PsiElement yamlKeyValueLastChild = yamlKeyValue.getLastChild();
                    if (yamlKeyValueLastChild instanceof YAMLMapping) {
                        for (YAMLKeyValue keyValue : ((YAMLMapping) yamlKeyValueLastChild).getKeyValues()) {
                            visitEnvironmentSquenceItems(consumer, keyValue);
                        }
                    }
                }

                visitEnvironmentSquenceItems(consumer, yamlKeyValue);
            }
        }

        for (String file : DOCKER_FILES) {
            for (PsiFile psiFile : FilenameIndex.getFilesByName(project, file, GlobalSearchScope.allScope(project))) {
                Matcher matcher = Pattern.compile("ENV\\s+([^\\s]*)\\s+").matcher(psiFile.getText());
                while(matcher.find()){
                    consumer.accept(Pair.create(matcher.group(1), psiFile));
                }
            }
        }
    }

    /**
     * environment:
     *   - FOOBAR=0
     */
    private static void visitEnvironmentSquenceItems(@NotNull Consumer<Pair<String, PsiElement>> consumer, @NotNull YAMLKeyValue yamlKeyValue) {
        YAMLKeyValue environment = YamlHelper.getYamlKeyValue(yamlKeyValue, "environment");
        if (environment != null) {
            for (YAMLSequenceItem yamlSequenceItem : YamlHelper.getSequenceItems(environment)) {
                YAMLValue value = yamlSequenceItem.getValue();
                if (value instanceof YAMLScalar) {
                    String textValue = ((YAMLScalar) value).getTextValue();
                    if (StringUtils.isNotBlank(textValue)) {
                        String[] split = textValue.split("=");
                        if (split.length > 1) {
                            consumer.accept(Pair.create(split[0], value));
                        }
                    }
                }
            }
        }
    }
}
