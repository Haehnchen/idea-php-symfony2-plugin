package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser.FunctionHash;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Navigates from a symfony/ux-css token in a css() hash to its declaration in the {@code ux_css} configuration of the
 * project, in YAML or PHP, or in the default Panda tokens of the bundle.
 * <p>
 * Example: <code>{{ css({ color: 'blue.500', p: 'md' }) }}</code>
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public class UxCssTokenGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final List<String> CONFIG_SECTIONS = List.of("tokens", "semantic_tokens");
    private static final String PANDA_TOKENS = "panda-tokens.json";
    private static final String PANDA_TOKENS_PATH = "ux-css/resources/" + PANDA_TOKENS;

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement element, int offset, Editor editor) {
        ASTNode node = element != null ? element.getNode() : null;
        if (node == null || node.getElementType() != TwigTokenTypes.STRING_TEXT || !Symfony2ProjectComponent.isEnabled(element)) {
            return null;
        }

        FunctionHash hash = TwigFunctionHashParser.parse(element);
        if (hash == null || hash.valueKey() == null) {
            return null;
        }

        Set<String> tokenNames = getTokenNames(element.getText());
        if (tokenNames.isEmpty()) {
            return null;
        }

        Project project = element.getProject();
        TwigExtension twigExtension = TwigExtensionParser.getFunctions(project).get(hash.functionName());
        String category = UxCssUtil.isCssFunction(twigExtension) ? UxCssUtil.getTokenCategory(project, twigExtension, hash) : null;
        if (category == null) {
            return null;
        }

        // Resolve the category roots once, then reuse them for exact names and modifier fallbacks.
        List<PsiElement> categoryRoots = new ArrayList<>();
        collectConfigCategoryRoots(project, category, categoryRoots);
        collectPandaCategoryRoots(project, category, categoryRoots);

        for (String token : tokenNames) {
            List<PsiElement> targets = new ArrayList<>();
            for (PsiElement root : categoryRoots) {
                if (root instanceof YAMLMapping mapping) {
                    collectYamlTargets(mapping, token, targets);
                } else if (root instanceof ArrayCreationExpression array) {
                    collectPhpTargets(array, token, targets);
                } else if (root instanceof JsonObject object) {
                    collectJsonTargets(object, token, targets);
                }
            }

            if (!targets.isEmpty()) {
                return targets.toArray(PsiElement.EMPTY_ARRAY);
            }
        }

        return null;
    }

    /**
     * Token names to look for, in order: the value itself, then without an opacity ({@code primary/50}) or a minus
     * sign ({@code -md}). The value comes first, because a token name can hold a slash, like {@code 1/2}.
     */
    @NotNull
    private static Set<String> getTokenNames(@NotNull String styleValue) {
        String token = UxCssUtil.stripImportant(styleValue);
        if (token.isEmpty() || token.startsWith("[")) {
            return Set.of();
        }

        Set<String> names = new LinkedHashSet<>();
        names.add(token);
        names.add(token.replaceFirst("/\\d{1,3}$", ""));
        names.add(StringUtils.removeStart(token, "-"));

        return names;
    }

    private static void collectConfigCategoryRoots(@NotNull Project project, @NotNull String category, @NotNull List<PsiElement> roots) {
        PsiManager psiManager = PsiManager.getInstance(project);

        for (VirtualFile virtualFile : UxCssConfigUtil.getConfigFiles(project)) {
            PsiFile psiFile = psiManager.findFile(virtualFile);

            if (psiFile instanceof YAMLFile yamlFile) {
                for (YAMLMapping config : UxCssConfigUtil.getYamlConfigs(yamlFile)) {
                    for (String section : CONFIG_SECTIONS) {
                        if (getYamlMapping(getYamlMapping(config, section), category) instanceof YAMLMapping mapping) {
                            roots.add(mapping);
                        }
                    }
                }
            } else if (psiFile instanceof PhpFile phpFile) {
                for (ArrayCreationExpression config : UxCssConfigUtil.getPhpConfigs(phpFile)) {
                    for (String section : CONFIG_SECTIONS) {
                        if (getPhpArray(getPhpArray(config, section), category) instanceof ArrayCreationExpression array) {
                            roots.add(array);
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private static YAMLMapping getYamlMapping(@Nullable YAMLMapping mapping, @NotNull String key) {
        YAMLKeyValue keyValue = mapping != null ? mapping.getKeyValueByKey(key) : null;
        return keyValue != null && keyValue.getValue() instanceof YAMLMapping value ? value : null;
    }

    @Nullable
    private static ArrayCreationExpression getPhpArray(@Nullable ArrayCreationExpression array, @NotNull String key) {
        return array != null && PhpElementsUtil.getArrayKeyValueMapWithValueAsPsiElement(array).get(key) instanceof ArrayCreationExpression value ? value : null;
    }

    private static void collectPhpTargets(@NotNull ArrayCreationExpression array, @NotNull String path, @NotNull List<PsiElement> targets) {
        for (ArrayHashElement hashElement : array.getHashElements()) {
            PhpPsiElement key = hashElement.getKey();
            if (key == null) {
                continue;
            }

            String name = key instanceof StringLiteralExpression string ? string.getContents() : key.getText();
            if (name.equals(path)) {
                targets.add(key);
            } else if (path.startsWith(name + ".") && hashElement.getValue() instanceof ArrayCreationExpression group) {
                collectPhpTargets(group, path.substring(name.length() + 1), targets);
            }
        }
    }

    /**
     * Follows a token path through nested groups; a key can hold a dot itself, like {@code 1.5} in {@code spacing}.
     */
    private static void collectYamlTargets(@NotNull YAMLMapping mapping, @NotNull String path, @NotNull List<PsiElement> targets) {
        for (YAMLKeyValue keyValue : mapping.getKeyValues()) {
            String key = keyValue.getKeyText();
            if (key.equals(path)) {
                targets.add(keyValue);
            } else if (path.startsWith(key + ".") && keyValue.getValue() instanceof YAMLMapping group) {
                collectYamlTargets(group, path.substring(key.length() + 1), targets);
            }
        }
    }

    private static void collectPandaCategoryRoots(@NotNull Project project, @NotNull String category, @NotNull List<PsiElement> roots) {
        for (VirtualFile virtualFile : FilenameIndex.getVirtualFilesByName(PANDA_TOKENS, GlobalSearchScope.allScope(project))) {
            if (!virtualFile.getPath().endsWith(PANDA_TOKENS_PATH) || !(PsiManager.getInstance(project).findFile(virtualFile) instanceof JsonFile jsonFile)) {
                continue;
            }

            JsonObject tokens = getObject(jsonFile.getTopLevelValue() instanceof JsonObject root ? root : null, "tokens");
            JsonObject categoryObject = getObject(tokens, category);
            if (categoryObject != null) {
                roots.add(categoryObject);
            }
        }
    }

    private static void collectJsonTargets(@NotNull JsonObject object, @NotNull String path, @NotNull List<PsiElement> targets) {
        for (JsonProperty property : object.getPropertyList()) {
            String key = property.getName();
            if (key.equals(path)) {
                targets.add(property);
            } else if (path.startsWith(key + ".") && property.getValue() instanceof JsonObject group) {
                collectJsonTargets(group, path.substring(key.length() + 1), targets);
            }
        }
    }

    @Nullable
    private static JsonObject getObject(@Nullable JsonObject object, @NotNull String name) {
        JsonProperty property = object != null ? object.findProperty(name) : null;
        return property != null && property.getValue() instanceof JsonObject value ? value : null;
    }
}
