package fr.adrienbrault.idea.symfony2plugin.dic.tags;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.xml.XmlServiceTag;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml.YamlServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlTagVisitor;
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
            return create(serviceId, (YAMLKeyValue) psiElement);
        } else if(psiElement instanceof XmlTag) {
            return create(serviceId, (XmlTag) psiElement);
        }

        return null;
    }

    @Nullable
    private static Collection<ServiceTagInterface> create(@NotNull final String serviceId, @NotNull YAMLKeyValue yamlHash) {

        final Collection<ServiceTagInterface> tags = new ArrayList<ServiceTagInterface>();

        YamlHelper.visitTagsOnServiceDefinition(yamlHash, new YamlTagVisitor() {
            @Override
            public void visit(@NotNull fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlServiceTag args) {

                String methodName = args.getAttribute("method");
                if (StringUtils.isBlank(methodName)) {
                    return;
                }

                ServiceTagInterface e = YamlServiceTag.create(serviceId, args.getYamlHash());
                if(e != null) {
                    tags.add(e);
                }
            }

        });

        return tags;
    }

    @Nullable
    private static Collection<ServiceTagInterface> create(@NotNull String serviceId, @NotNull XmlTag xmlTag) {

        final Collection<ServiceTagInterface> tags = new ArrayList<ServiceTagInterface>();

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
