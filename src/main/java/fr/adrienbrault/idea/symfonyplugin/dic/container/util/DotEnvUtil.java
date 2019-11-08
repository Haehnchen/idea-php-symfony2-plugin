package fr.adrienbrault.idea.symfonyplugin.dic.container.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
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
     * @param parameter "%env(FOOBAR)%", "%env(resolve:DB)%"
     */
    @NotNull
    public static Collection<PsiElement> getEnvironmentVariableTargetsForParameter(@NotNull Project project, @NotNull String parameter) {
        if(parameter.length() < 7 || !parameter.startsWith("%env(") || !parameter.endsWith(")%")) {
            return Collections.emptyList();
        }

        String parameterName = parameter.substring(5, parameter.length() - 2);

        // https://github.com/symfony/symfony/pull/23901 => RegisterEnvVarProcessorsPass
        // '%env(int:DATABASE_PORT)%'
        // '%env(resolve:int:foo:DB)%'
        Matcher matcher = Pattern.compile("^[\\w-_^:]+:(.*)$", Pattern.MULTILINE).matcher(parameterName);
        if(matcher.find()){
            parameterName = matcher.group(1);
        }

        return DotEnvUtil.getEnvironmentVariableTargets(project, parameterName);
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
        Set<VirtualFile> files = new HashSet<>(
            FilenameIndex.getAllFilesByExt(project, "env", GlobalSearchScope.allScope(project))
        );

        // try to find some env's ;)
        for (String file : new String[]{".env", ".env.dist", ".env.test", ".env.local"}) {
            files.addAll(FilenameIndex.getVirtualFilesByName(project, file, GlobalSearchScope.allScope(project)));
        }

        // search root directory for all ".env*" files
        VirtualFile projectDir = VfsUtil.findRelativeFile(project.getBaseDir());
        if (projectDir != null) {
            for (VirtualFile child : projectDir.getChildren()) {
                if (child.getName().startsWith(".env")) {
                    files.add(child);
                }
            }
        }

        for (VirtualFile virtualFile : files) {
            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if(file == null) {
                continue;
            }

            Properties variables = new Properties();
            try {
                variables.load(virtualFile.getInputStream());
            } catch (IOException e) {
                continue;
            }

            for (Map.Entry<Object, Object> variable : variables.entrySet()) {
                Object key = variable.getKey();
                if(key instanceof String) {
                    consumer.accept(Pair.create((String) key, file));
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
                // ENV DOCKERFILE_FOO /bar
                Matcher matcher = Pattern.compile("ENV\\s+([^\\s]*)\\s+").matcher(psiFile.getText());
                while(matcher.find()){
                    consumer.accept(Pair.create(matcher.group(1), psiFile));
                }

                // ENV ADMIN_USER_DOCKERFILE="mark"
                // ENV ADMIN_USER_DOCKERFILE ="mark"
                matcher = Pattern.compile("ENV\\s+([\\w+]*)\\s*=").matcher(psiFile.getText());
                while(matcher.find()){
                    consumer.accept(Pair.create(matcher.group(1), psiFile));
                }
            }
        }
    }

    /**
     * environment:
     *   - FOOBAR=0
     *
     * environment:
     *   FOOBAR: 0
     */
    private static void visitEnvironmentSquenceItems(@NotNull Consumer<Pair<String, PsiElement>> consumer, @NotNull YAMLKeyValue yamlKeyValue) {
        YAMLKeyValue environment = YamlHelper.getYamlKeyValue(yamlKeyValue, "environment");
        if (environment != null) {
            // FOOBAR=0
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

            // FOOBAR: 0
            YAMLMapping childOfType = PsiTreeUtil.getChildOfType(environment, YAMLMapping.class);
            if (childOfType != null) {
                for (Map.Entry<String, YAMLValue> entry : YamlHelper.getYamlArrayKeyMap(childOfType).entrySet()) {
                    consumer.accept(Pair.create(entry.getKey(), entry.getValue()));
                }
            }
        }
    }
}
