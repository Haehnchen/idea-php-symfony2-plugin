package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses console commands from compiled container XML by collecting services tagged with
 * {@code console.command} that carry the command name directly as a tag attribute:
 *
 * <pre>{@code
 * <service id="doctrine.cache_clear_metadata_command" class="Doctrine\ORM\...\MetadataCommand">
 *   <tag name="console.command" command="doctrine:cache:clear-metadata"/>
 * </service>
 * }</pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConsoleCommandServiceParser extends AbstractServiceParser {

    private final Map<String, String> commands = new HashMap<>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@class]/tag[@name='console.command'][@command]";
    }

    @Override
    public void parser(@NotNull InputStream inputStream, @NotNull VirtualFile sourceFile, @NotNull Project project) {
        NodeList nodeList = this.parserer(inputStream);
        if (nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element tagElement = (Element) nodeList.item(i);
            String commandName = tagElement.getAttribute("command");
            if (commandName.isBlank()) {
                continue;
            }

            Element serviceElement = (Element) tagElement.getParentNode();
            String className = serviceElement.getAttribute("class");
            if (className.isBlank()) {
                continue;
            }

            String fqn = className.startsWith("\\") ? className : "\\" + className;
            commands.put(commandName, fqn);
        }
    }

    /**
     * Returns a map of command name → fully-qualified class name for all parsed commands.
     */
    public Map<String, String> getCommands() {
        return Collections.unmodifiableMap(commands);
    }
}
