package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.config.php.PhpConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.UxCssConfigStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Finds the symfony/ux-css configuration of a YAML or PHP config file, whatever its name.
 * <p>
 * Examples: <code>ux_css: { tokens: ... }</code>, <code>when@dev: { ux_css: ... }</code>,
 * <code>return App::config(['ux_css' => [...]])</code>, <code>$container->extension('ux_css', [...])</code>
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public final class UxCssConfigUtil {
    public static final String ROOT_KEY = "ux_css";

    private UxCssConfigUtil() {
    }

    @NotNull
    public static Collection<VirtualFile> getConfigFiles(@NotNull Project project) {
        return FileBasedIndex.getInstance().getContainingFiles(UxCssConfigStubIndex.KEY, ROOT_KEY, GlobalSearchScope.projectScope(project));
    }

    /**
     * @return the mappings under {@code ux_css}, at the root of the file or of a {@code when@...} block
     */
    @NotNull
    public static List<YAMLMapping> getYamlConfigs(@NotNull YAMLFile yamlFile) {
        List<YAMLMapping> configs = new ArrayList<>();

        for (YAMLKeyValue keyValue : YamlHelper.getTopLevelKeyValues(yamlFile)) {
            String key = keyValue.getKeyText();
            if (ROOT_KEY.equals(key) && keyValue.getValue() instanceof YAMLMapping config) {
                configs.add(config);
            } else if (key.startsWith("when@")
                && keyValue.getValue() instanceof YAMLMapping conditional
                && conditional.getKeyValueByKey(ROOT_KEY) instanceof YAMLKeyValue nested
                && nested.getValue() instanceof YAMLMapping config
            ) {
                configs.add(config);
            }
        }

        return configs;
    }

    /**
     * @return the arrays under {@code 'ux_css'} in a returned config array, and the arrays given to {@code ->extension('ux_css', [...])}
     */
    @NotNull
    public static List<ArrayCreationExpression> getPhpConfigs(@NotNull PhpFile phpFile) {
        // Shared by navigation, gutter markers and indexing; depend only on this file's PSI.
        return CachedValuesManager.getCachedValue(phpFile, () ->
            CachedValueProvider.Result.create(collectPhpConfigs(phpFile), phpFile));
    }

    @NotNull
    private static List<ArrayCreationExpression> collectPhpConfigs(@NotNull PhpFile phpFile) {
        List<ArrayCreationExpression> configs = new ArrayList<>();

        PsiTreeUtil.processElements(phpFile, element -> {
            if (element instanceof ArrayHashElement hashElement
                && hashElement.getKey() instanceof StringLiteralExpression key
                && ROOT_KEY.equals(key.getContents())
                && (PhpConfigUtil.isRootConfigKey(key) || PhpConfigUtil.isConditionalConfigKey(key))
                && hashElement.getValue() instanceof ArrayCreationExpression config
            ) {
                configs.add(config);
            }

            if (element instanceof MethodReference methodReference && "extension".equals(methodReference.getName())) {
                PsiElement[] parameters = methodReference.getParameters();
                if (parameters.length >= 2
                    && parameters[0] instanceof StringLiteralExpression name
                    && ROOT_KEY.equals(name.getContents())
                    && parameters[1] instanceof ArrayCreationExpression config
                ) {
                    configs.add(config);
                }
            }

            return true;
        });

        return List.copyOf(configs);
    }
}
