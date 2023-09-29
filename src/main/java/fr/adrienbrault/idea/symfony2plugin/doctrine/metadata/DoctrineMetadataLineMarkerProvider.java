package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTokenType;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> results) {
        // we need project element; so get it from first item
        if(psiElements.size() == 0) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        PsiFile containingFile = psiElements.get(0).getContainingFile();
        if (containingFile == null || !XmlHelper.isXmlFileExtension(containingFile)) {
            return;
        }

        for(PsiElement psiElement: psiElements) {
            if(psiElement.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
                continue;
            }

            PsiElement xmlAttributeValue = psiElement.getParent();
            if(xmlAttributeValue instanceof XmlAttributeValue && (DoctrineMetadataPattern.getXmlTargetDocumentClass().accepts(xmlAttributeValue) || DoctrineMetadataPattern.getXmlTargetEntityClass().accepts(xmlAttributeValue) || DoctrineMetadataPattern.getEmbeddableNameClassPattern().accepts(xmlAttributeValue))) {
                attachXmlRelationMarker(psiElement, (XmlAttributeValue) xmlAttributeValue, results);
            }
        }
    }

    private void attachXmlRelationMarker(@NotNull PsiElement target, @NotNull XmlAttributeValue psiElement, @NotNull Collection<? super LineMarkerInfo<?>> results) {
        String value = psiElement.getValue();
        if(StringUtils.isBlank(value)) {
            return;
        }

        Collection<PhpClass> classesInterface = DoctrineMetadataUtil.getClassInsideScope(psiElement, value);
        if(classesInterface.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
            setTargets(classesInterface).
            setTooltipText("Navigate to class");

        results.add(builder.createLineMarkerInfo(target));
    }

}
