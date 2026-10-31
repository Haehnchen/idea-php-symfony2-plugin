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
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
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
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Navigates from a symfony/ux-css token in a css() hash to its declaration in the {@code ux_css} configuration of the
 * project, in YAML or PHP, or in the default Panda tokens of the bundle.
 * <p>
 * Example: <code>{{ css({ color: 'blue.500', p: 'md' }) }}</code>
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public class UxCssTokenGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final Pattern TOKEN_TYPE = Pattern.compile("([A-Z][A-Za-z]*)Token\\d*");
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

        Project project = element.getProject();
        TwigExtension twigExtension = TwigExtensionParser.getFunctions(project).get(hash.functionName());
        String category = UxCssUtil.isCssFunction(twigExtension) ? getTokenCategory(project, twigExtension, hash) : null;
        if (category == null) {
            return null;
        }

        for (String token : getTokenNames(element.getText())) {
            List<PsiElement> targets = new ArrayList<>();
            collectConfigTargets(project, category, token, targets);
            collectPandaTargets(project, category, token, targets);

            if (!targets.isEmpty()) {
                return targets.toArray(PsiElement.EMPTY_ARRAY);
            }
        }

        return null;
    }

    /**
     * Reads the category from the type alias of the key in {@code config/reference_css.php}, e.g. {@code spacing} for
     * <code>p?: SpacingToken|...</code>.
     */
    @Nullable
    private static String getTokenCategory(@NotNull Project project, @NotNull TwigExtension twigExtension, @NotNull FunctionHash hash) {
        List<String> keyPath = hash.keyPath();

        for (PhpNamedElement element : PhpElementsUtil.getPsiElementsBySignature(project, twigExtension.getSignature())) {
            if (!(element instanceof Function function)) {
                continue;
            }

            String category = getTokenCategory(function, hash.argumentIndex(), keyPath, hash.valueKey());

            // a conditional value, e.g. color: { base: '...' }
            if (category == null && !keyPath.isEmpty()) {
                category = getTokenCategory(function, hash.argumentIndex(), keyPath.subList(0, keyPath.size() - 1), keyPath.getLast());
            }

            if (category != null) {
                return category;
            }
        }

        return null;
    }

    @Nullable
    private static String getTokenCategory(@NotNull Function function, int parameterIndex, @NotNull List<String> keyPath, @NotNull String key) {
        for (String typeName : PhpDocArrayShapeUtil.getParameterValueTypeNames(function, parameterIndex, keyPath, key)) {
            Matcher matcher = TOKEN_TYPE.matcher(typeName);
            if (matcher.matches()) {
                return StringUtils.uncapitalize(matcher.group(1));
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

    private static void collectConfigTargets(@NotNull Project project, @NotNull String category, @NotNull String token, @NotNull List<PsiElement> targets) {
        PsiManager psiManager = PsiManager.getInstance(project);

        for (VirtualFile virtualFile : UxCssConfigUtil.getConfigFiles(project)) {
            PsiFile psiFile = psiManager.findFile(virtualFile);

            if (psiFile instanceof YAMLFile yamlFile) {
                for (YAMLMapping config : UxCssConfigUtil.getYamlConfigs(yamlFile)) {
                    for (String section : CONFIG_SECTIONS) {
                        if (getYamlMapping(getYamlMapping(config, section), category) instanceof YAMLMapping mapping) {
                            collectYamlTargets(mapping, token, targets);
                        }
                    }
                }
            } else if (psiFile instanceof PhpFile phpFile) {
                for (ArrayCreationExpression config : UxCssConfigUtil.getPhpConfigs(phpFile)) {
                    for (String section : CONFIG_SECTIONS) {
                        if (getPhpArray(getPhpArray(config, section), category) instanceof ArrayCreationExpression array) {
                            collectPhpTargets(array, token, targets);
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

    private static void collectPandaTargets(@NotNull Project project, @NotNull String category, @NotNull String token, @NotNull List<PsiElement> targets) {
        for (VirtualFile virtualFile : FilenameIndex.getVirtualFilesByName(PANDA_TOKENS, GlobalSearchScope.allScope(project))) {
            if (!virtualFile.getPath().endsWith(PANDA_TOKENS_PATH) || !(PsiManager.getInstance(project).findFile(virtualFile) instanceof JsonFile jsonFile)) {
                continue;
            }

            JsonObject tokens = getObject(jsonFile.getTopLevelValue() instanceof JsonObject root ? root : null, "tokens");
            JsonObject categoryObject = getObject(tokens, category);
            if (categoryObject != null) {
                collectJsonTargets(categoryObject, token, targets);
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
