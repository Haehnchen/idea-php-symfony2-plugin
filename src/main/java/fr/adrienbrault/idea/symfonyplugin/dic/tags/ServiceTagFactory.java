package fr.adrienbrault.idea.symfonyplugin.dic.tags;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfonyplugin.dic.tags.xml.XmlServiceTag;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceTagFactory {

    @Nullable
    public static Collection<ServiceTagInterface> create(@NotNull String serviceId, @NotNull PsiElement psiElement) {

        if(psiElement instanceof YAMLKeyValue) {
            return create((YAMLKeyValue) psiElement);
        } else if(psiElement instanceof XmlTag) {
            return create(serviceId, (XmlTag) psiElement);
        }

        return null;
    }

    @Nullable
    private static Collection<ServiceTagInterface> create(@NotNull YAMLKeyValue yamlHash) {

        final Collection<ServiceTagInterface> tags = new ArrayList<>();

        YamlHelper.visitTagsOnServiceDefinition(yamlHash, args -> {
            String methodName = args.getAttribute("method");
            if (StringUtils.isBlank(methodName)) {
                return;
            }

            tags.add(args);
        });

        return tags;
    }

    @Nullable
    private static Collection<ServiceTagInterface> create(@NotNull String serviceId, @NotNull XmlTag xmlTag) {

        final Collection<ServiceTagInterface> tags = new ArrayList<>();

        for (XmlTag tag : xmlTag.findSubTags("tag")) {

            String name = tag.getAttributeValue("name");
            if(name == null) {
                continue;
            }

            ServiceTagInterface serviceTagInterface = XmlServiceTag.create(serviceId, tag);
            if(serviceTagInterface == null) {
                continue;
            }

            tags.add(serviceTagInterface);
        }

        return tags;
    }
}
