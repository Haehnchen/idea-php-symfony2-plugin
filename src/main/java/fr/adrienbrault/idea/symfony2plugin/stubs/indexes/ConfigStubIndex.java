package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.ConfigIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.*;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigStubIndex extends FileBasedIndexExtension<String, ConfigIndex> {
    public static final ID<String, ConfigIndex> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.config_stub_index");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final int MAX_FILE_BYTE_SIZE = 2097152;
    private static final ObjectStreamDataExternalizer<ConfigIndex> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public ID<String, ConfigIndex> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, ConfigIndex, FileContent> getIndexer() {
        return new DataIndexer<>() {
            @Override
            public @NotNull Map<String, ConfigIndex> map(@NotNull FileContent inputData) {
                if (!(inputData.getPsiFile() instanceof YAMLFile yamlFile) ||
                    !Symfony2ProjectComponent.isEnabledForIndex(yamlFile.getProject()) ||
                    !isValidForIndex(inputData, yamlFile)
                ) {
                    return Collections.emptyMap();
                }

                Map<String, ConfigIndex> map = new HashMap<>();
                TreeMap<String, TreeMap<String, String>> configs = new TreeMap<>();
                Set<String> anonymousTemplateDirectory = new HashSet<>();

                for (YAMLKeyValue yamlKeyValue : YamlHelper.getTopLevelKeyValues(yamlFile)) {
                    String keyText = yamlKeyValue.getKeyText();
                    if ("twig_component".equals(keyText)) {
                        visitKey(yamlKeyValue, configs, anonymousTemplateDirectory);
                    }

                    if (keyText.startsWith("when@")) {
                        YAMLValue value = yamlKeyValue.getValue();
                        if (value instanceof YAMLMapping) {
                            for (YAMLKeyValue yamlKeyValue2 : ((YAMLMapping) value).getKeyValues()) {
                                String keyText2 = yamlKeyValue2.getKeyText();
                                if ("twig_component".equals(keyText2)) {
                                    visitKey(yamlKeyValue2, configs, anonymousTemplateDirectory);
                                }
                            }
                        }
                    }
                }

                if (!configs.isEmpty()) {
                    map.put("twig_component_defaults", new ConfigIndex("twig_component_defaults", configs, Collections.emptySet()));
                }

                if (!anonymousTemplateDirectory.isEmpty()) {
                    map.put("anonymous_template_directory", new ConfigIndex("anonymous_template_directory", new TreeMap<>(), anonymousTemplateDirectory));
                }

                return map;
            }

            private static void visitKey(@NotNull YAMLKeyValue yamlKeyValue, @NotNull TreeMap<String, TreeMap<String, String>> configs, @NotNull Set<String> anonymousTemplateDirectory) {
                YAMLValue value = yamlKeyValue.getValue();
                if (value instanceof YAMLMapping yamlMapping) {
                    YAMLKeyValue defaults = YamlHelper.getYamlKeyValue(yamlMapping, "defaults");
                    if (defaults != null) {
                        YAMLValue value1 = defaults.getValue();
                        if (value1 instanceof YAMLMapping yamlMapping1) {
                            for (YAMLKeyValue keyValue : yamlMapping1.getKeyValues()) {
                                String keyText1 = keyValue.getKeyText();

                                YAMLValue value2 = keyValue.getValue();
                                if (value2 instanceof YAMLQuotedText || value2 instanceof YAMLPlainTextImpl) {
                                    String s = PsiElementUtils.trimQuote(value2.getText());
                                    if (!StringUtils.isBlank(s)) {
                                        TreeMap<String, String> items = new TreeMap<>();
                                        items.put("template_directory", s);
                                        configs.put(keyText1, items);
                                    }
                                } else if (value2 instanceof YAMLMapping yamlMapping2) {
                                    TreeMap<String, String> items = new TreeMap<>();

                                    String templateDirectory = YamlHelper.getYamlKeyValueAsString(yamlMapping2, "template_directory");
                                    if (templateDirectory == null) {
                                        continue;
                                    }

                                    items.put("template_directory", templateDirectory);

                                    String namePrefix = YamlHelper.getYamlKeyValueAsString(yamlMapping2, "name_prefix");
                                    if (namePrefix != null) {
                                        items.put("name_prefix", namePrefix);
                                    }

                                    configs.put(keyText1, items);
                                }
                            }
                        }
                    }

                    String templateDirectory = YamlHelper.getYamlKeyValueAsString(yamlMapping, "anonymous_template_directory");
                    if (templateDirectory != null) {
                        anonymousTemplateDirectory.add(templateDirectory);
                    }
                }
            }
        };
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public @NotNull DataExternalizer<ConfigIndex> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return virtualFile -> virtualFile.getFileType() == YAMLFileType.YML;
    }


    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    private static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {
        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        // is Test file in path name
        String relativePath = VfsUtil.getRelativePath(inputData.getFile(), ProjectUtil.getProjectDir(inputData.getProject()), '/');
        if(relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Tests/") || relativePath.contains("/Fixture/") || relativePath.contains("/Fixtures/"))) {
            return false;
        }

        if(inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
            return false;
        }

        return true;
    }
}
