package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.PhpPresentationUtil;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TwigExtensionLookupElement extends LookupElement {

    final private TwigExtension twigExtension;
    final private String name;
    final private Project project;

    public TwigExtensionLookupElement(Project project, String name, TwigExtension twigExtension) {
        this.project = project;
        this.name = name;
        this.twigExtension = twigExtension;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return name;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setIcon(TwigExtensionParser.getIcon(this.twigExtension.getTwigExtensionType()));

        String signature = this.twigExtension.getSignature();
        if(signature != null) {
            Collection<? extends PhpNamedElement> phpNamedElements = PhpIndex.getInstance(this.project).getBySignature(signature);
            if(phpNamedElements.size() > 0) {

                PhpNamedElement function = phpNamedElements.iterator().next();
                if(function instanceof FunctionImpl) {
                    List<Parameter> parameters = new LinkedList<>(Arrays.asList(((FunctionImpl) function).getParameters()));
                    if(this.twigExtension.getOption("needs_context") != null && parameters.size() > 0) {
                        parameters.remove(0);
                    }
                    if(this.twigExtension.getOption("needs_environment") != null && parameters.size() > 0) {
                        parameters.remove(0);
                    }
                    presentation.setTailText(PhpPresentationUtil.formatParameters(null, parameters.toArray(new Parameter[parameters.size()])).toString(), true);
                }

            }
        }

        presentation.setItemText(name);
        presentation.setTypeText(StringUtils.camelize(this.twigExtension.getType().toLowerCase()));
        presentation.setTypeGrayed(true);

    }

}