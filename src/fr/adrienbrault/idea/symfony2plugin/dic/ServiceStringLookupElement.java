package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceStringLookupElement extends LookupElement {

    @NotNull
    private ContainerService containerService;

    private boolean boldText = false;

    public ServiceStringLookupElement(@NotNull ContainerService containerService) {
        this.containerService = containerService;
    }

    public ServiceStringLookupElement(@NotNull ContainerService containerService, boolean boldText) {
        this(containerService);
        this.boldText = boldText;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return containerService.getName();
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeGrayed(true);

        String className = getClassName(containerService);
        if(className != null) {
            presentation.setTypeText(StringUtils.strip(className, "\\"));
        }

        // private or non container services
        if(className == null || containerService.isWeak()) {
            presentation.setIcon(Symfony2Icons.SERVICE_OPACITY);
        } else {
            presentation.setIcon(Symfony2Icons.SERVICE);
        }

        if(this.containerService.isPrivate()) {
            presentation.setIcon(Symfony2Icons.SERVICE_PRIVATE_OPACITY);
        }

        presentation.setItemTextBold(this.boldText);
        if(this.boldText) {
            presentation.setTypeGrayed(false);
            presentation.setItemTextUnderlined(true);
        }

        if(containerService.getService() != null) {
            presentation.setStrikeout(containerService.getService().isDeprecated());
        }
    }

    @Nullable
    private String getClassName(@NotNull ContainerService containerService) {
        return ContainerUtil.find(containerService.getClassNames(), s -> s != null);
    }
}
