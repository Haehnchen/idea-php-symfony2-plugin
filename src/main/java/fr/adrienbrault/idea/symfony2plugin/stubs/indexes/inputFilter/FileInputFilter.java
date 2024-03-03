package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

/**
 * Files which are allowed for cusomng on index
 *
 * Its a performance issue indexing too many files
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileInputFilter {
    public static final FileBasedIndex.InputFilter XML_YAML_PHP = new XmlYamlAndPhpFileInputFilter();
    public static final FileBasedIndex.InputFilter XML_YAML = new XmlAndYamlFileInputFilter();

    private static class XmlAndYamlFileInputFilter implements FileBasedIndex.InputFilter {
        @Override
        public boolean acceptInput(@NotNull VirtualFile file) {
            FileType fileType = file.getFileType();

            // "xsd" and other file types are inside file tye, filter by extension
            if (fileType == XmlFileType.INSTANCE) {
                String extension = file.getExtension();
                return "xml".equalsIgnoreCase(extension);
            }

            return fileType == YAMLFileType.YML;
        }
    }

    private static class XmlYamlAndPhpFileInputFilter implements FileBasedIndex.InputFilter {
        @Override
        public boolean acceptInput(@NotNull VirtualFile file) {
            FileType fileType = file.getFileType();

            // "xsd" and other file types are inside file tye, filter by extension
            if (fileType == XmlFileType.INSTANCE) {
                String extension = file.getExtension();
                return "xml".equalsIgnoreCase(extension);
            }

            return fileType == YAMLFileType.YML || fileType == PhpFileType.INSTANCE;
        }
    }
}
