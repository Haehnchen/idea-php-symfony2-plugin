package fr.adrienbrault.idea.symfonyplugin.templating.dict;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.PhpPresentationUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfonyplugin.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtensionLookupElement extends LookupElement {
    @NotNull
    final private TwigExtension twigExtension;

    @NotNull
    final private String name;

    @NotNull
    final private Project project;

    public TwigExtensionLookupElement(@NotNull Project project, @NotNull String name, @NotNull TwigExtension twigExtension) {
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
    public void handleInsert(InsertionContext context) {
        if(twigExtension.getTwigExtensionType() == TwigExtensionParser.TwigExtensionType.SIMPLE_FUNCTION) {
            TwigExtensionInsertHandler.getInstance().handleInsert(context, this, twigExtension);
        }

        super.handleInsert(context);
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setIcon(TwigExtensionParser.getIcon(this.twigExtension.getTwigExtensionType()));

        buildTailText(presentation);

        presentation.setItemText(name);
        presentation.setTypeText(StringUtils.camelize(this.twigExtension.getType().toLowerCase()));
        presentation.setTypeGrayed(true);
    }

    private void buildTailText(@NotNull LookupElementPresentation presentation) {
        if(this.twigExtension.getTwigExtensionType() == TwigExtensionParser.TwigExtensionType.SIMPLE_TEST) {
            return;
        }

        String signature = this.twigExtension.getSignature();
        if(signature == null) {
            return;
        }

        Collection<? extends PhpNamedElement> phpNamedElements = PhpIndex.getInstance(this.project).getBySignature(signature);
        if(phpNamedElements.size() == 0) {
            return;
        }

        PhpNamedElement function = phpNamedElements.iterator().next();
        if(function instanceof Function) {
            List<Parameter> parameters = new LinkedList<>(Arrays.asList(((Function) function).getParameters()));

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