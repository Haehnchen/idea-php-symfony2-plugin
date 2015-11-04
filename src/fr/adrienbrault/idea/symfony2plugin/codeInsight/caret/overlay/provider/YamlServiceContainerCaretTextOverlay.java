package fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.provider;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpPresentationUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.CaretTextOverlay;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.CaretTextOverlayArguments;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.CaretTextOverlayElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.util.CaretTextOverlayUtil;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlServiceContainerCaretTextOverlay implements CaretTextOverlay {
    @Nullable
    @Override
    public CaretTextOverlayElement getOverlay(@NotNull CaretTextOverlayArguments args) {

        // @service
        if(YamlElementPatternHelper.getServiceParameterDefinition().accepts(args.getPsiElement())) {
            CaretTextOverlayElement argumentOverlay = getArgumentValueOverlay(args.getPsiElement(), args);
            if(argumentOverlay != null) {
                return argumentOverlay;
            }
        }

        // %parameter%
        if(YamlElementPatternHelper.getServiceDefinition().accepts(args.getPsiElement())) {
            CaretTextOverlayElement argumentOverlay = getServiceNameOverlay(args.getPsiElement());
            if(argumentOverlay != null) {
                return argumentOverlay;
            }
        }

        // fo<caret>o:
        //   class: foo
        if(PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).accepts(args.getPsiElement())) {
            CaretTextOverlayElement argumentOverlay = getClassConstructorSignature(args.getPsiElement(), args);
            if(argumentOverlay != null) {
                return argumentOverlay;
            }
        }

        return null;
    }

    @Nullable
    private CaretTextOverlayElement getArgumentValueOverlay(@NotNull PsiElement psiElement, @NotNull CaretTextOverlayArguments args) {
        String value = PsiElementUtils.getText(psiElement);
        if(!YamlHelper.isValidParameterName(value)) {
            return null;
        }

        String s = ContainerCollectionResolver.resolveParameter(args.getProject(), value);
        if(s == null) {
            return null;
        }

        return new CaretTextOverlayElement(s);
    }


    @Nullable
    private CaretTextOverlayElement getServiceNameOverlay(@NotNull PsiElement psiElement) {
        String valueTrim = YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.trimQuote(PsiElementUtils.getText(psiElement)));
        if(valueTrim == null) {
            return null;
        }

        String serviceClass = ContainerCollectionResolver.resolveService(psiElement.getProject(), valueTrim);
        if(serviceClass == null) {
            return null;
        }

        return new CaretTextOverlayElement(serviceClass);
    }

    @Nullable
    private CaretTextOverlayElement getClassConstructorSignature(@NotNull PsiElement psiElement, @NotNull CaretTextOverlayArguments args) {

        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof YAMLKeyValue)) {
            return null;
        }

        String aClass = YamlHelper.getYamlKeyValueAsString((YAMLKeyValue) parent, "class");
        if(aClass == null) {
            return null;
        }

        return CaretTextOverlayUtil.getCaretTextOverlayForServiceConstructor(args.getProject(), aClass);
    }

    @Override
    public boolean accepts(@NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType() == YAMLFileType.YML;
    }
}
