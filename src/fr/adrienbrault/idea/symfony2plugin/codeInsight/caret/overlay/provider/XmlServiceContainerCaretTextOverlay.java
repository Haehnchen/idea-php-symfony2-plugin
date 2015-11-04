package fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.provider;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.CaretTextOverlay;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.CaretTextOverlayArguments;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.CaretTextOverlayElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.util.CaretTextOverlayUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceContainerCaretTextOverlay implements CaretTextOverlay {

    @Nullable
    @Override
    public CaretTextOverlayElement getOverlay(@NotNull CaretTextOverlayArguments args) {

        PsiElement parent = args.getPsiElement().getParent();
        if(parent instanceof XmlTag && ("service".equals(((XmlTag) parent).getName()))) {
            CaretTextOverlayElement overlay = getParameterValueOverlay((XmlTag) parent, args);
            if(overlay != null) {
                return overlay;
            }
        } else if(getServiceClassScopePattern().accepts(parent)) {
            // <service id="aa<caret>a">
            // <service i<caret>d="aaa">
            XmlTag xmlTag = PsiTreeUtil.getParentOfType(parent, XmlTag.class);
            if(xmlTag != null && "service".equals(xmlTag.getName())) {
                CaretTextOverlayElement overlay = getParameterValueOverlay(xmlTag, args);
                if(overlay != null) {
                    return overlay;
                }
            }
        }

        if(parent instanceof XmlAttributeValue) {
            if(XmlHelper.getArgumentServiceIdPattern().accepts(parent)) {
                CaretTextOverlayElement overlay = getServiceInstanceOverlay((XmlAttributeValue) parent, args);
                if(overlay != null) {
                    return overlay;
                }
            }

            return null;
        } else if(parent instanceof XmlText) {
            // <foo>a<caret>a</foo>
            if(XmlHelper.getArgumentValuePattern().accepts(args.getPsiElement())) {
                CaretTextOverlayElement overlay = getParameterValueOverlay((XmlText) parent, args);
                if(overlay != null) {
                    return overlay;
                }
            }

            return null;
        }

        return null;
    }

    @NotNull
    private ElementPattern<XmlElement> getServiceClassScopePattern() {
        return XmlPatterns.or(
            XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns
                        .xmlAttribute()
                        .withParent(XmlPatterns
                                .xmlTag().withName("service")
                        )
                ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern()),
            XmlPatterns.xmlAttribute()
                .withParent(XmlPatterns
                        .xmlTag().withName("service")
                ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern())
        );
    }

    private CaretTextOverlayElement getServiceInstanceOverlay(@NotNull XmlAttributeValue psiElement, @NotNull CaretTextOverlayArguments args) {

        final String value = psiElement.getValue();
        if(StringUtils.isBlank(value)) {
            return null;
        }

        PhpClass serviceClass = ServiceUtil.getServiceClass(args.getProject(), value);
        if(serviceClass == null) {
            return null;
        }

        final String service = serviceClass.getPresentableFQN();
        if(service == null) {
            return null;
        }

        return new CaretTextOverlayElement(service);
    }

    private CaretTextOverlayElement getParameterValueOverlay(@NotNull XmlText psiElement, @NotNull CaretTextOverlayArguments args) {

        final String value = psiElement.getValue();
        if(StringUtils.isBlank(value) || !(value.startsWith("%") && value.endsWith("%"))) {
            return null;
        }

        String parameter = ContainerCollectionResolver.resolveParameter(args.getProject(), value);
        if(parameter == null) {
            return null;
        }

        return new CaretTextOverlayElement(parameter);
    }

    private CaretTextOverlayElement getParameterValueOverlay(@NotNull XmlTag xmlTag, @NotNull CaretTextOverlayArguments args) {
        String aClass = xmlTag.getAttributeValue("class");
        if(aClass == null || StringUtils.isBlank(aClass)) {
            return null;
        }

        return CaretTextOverlayUtil.getCaretTextOverlayForServiceConstructor(args.getProject(), aClass);
    }

    @Override
    public boolean accepts(@NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType() == XmlFileType.INSTANCE;
    }
}
