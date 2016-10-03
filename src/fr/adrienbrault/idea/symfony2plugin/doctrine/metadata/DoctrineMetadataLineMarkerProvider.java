package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> results) {

        // we need project element; so get it from first item
        if(psiElements.size() == 0) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for(PsiElement psiElement: psiElements) {
            if(psiElement instanceof XmlAttributeValue && (DoctrineMetadataPattern.getXmlTargetDocumentClass().accepts(psiElement) || DoctrineMetadataPattern.getXmlTargetEntityClass().accepts(psiElement) || DoctrineMetadataPattern.getEmbeddableNameClassPattern().accepts(psiElement))) {
                attachXmlRelationMarker((XmlAttributeValue) psiElement, results);
            }
        }
    }

    private void attachXmlRelationMarker(@NotNull XmlAttributeValue psiElement, @NotNull Collection<LineMarkerInfo> results) {
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

        results.add(builder.createLineMarkerInfo(psiElement));
    }

}
