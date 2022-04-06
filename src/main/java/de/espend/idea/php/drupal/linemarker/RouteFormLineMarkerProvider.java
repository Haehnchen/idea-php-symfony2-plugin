package de.espend.idea.php.drupal.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.utils.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteFormLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo<?>  getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> results) {
        if(psiElements.size() == 0) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if(!DrupalProjectComponent.isEnabled(project)) {
            return;
        }

        for (PsiElement psiElement : psiElements) {
            collectRouteInlineClasses(results, project, psiElement);
        }
    }

    private void collectRouteInlineClasses(@NotNull Collection<? super LineMarkerInfo<?>> results, @NotNull Project project, @NotNull PsiElement psiElement) {

        if(!(YamlElementPatternHelper.getSingleLineScalarKey("_form").accepts(psiElement) ||
            YamlElementPatternHelper.getSingleLineScalarKey("_entity_form").accepts(psiElement))
            ) {
            return;
        }

        PsiElement yamlScalar = psiElement.getParent();
        if(!(yamlScalar instanceof YAMLScalar)) {
            return;
        }

        String textValue = ((YAMLScalar) yamlScalar).getTextValue();

        Collection<PhpClass> classesInterface = new ArrayList<>(PhpElementsUtil.getClassesInterface(project, textValue));
        classesInterface.addAll(IndexUtil.getFormClassForId(project, textValue));

        if(classesInterface.size() == 0) {
            return;
        }

        PsiElement yamlKeyValue = yamlScalar.getParent();
        if(!(yamlKeyValue instanceof YAMLKeyValue)) {
            return;
        }

        YAMLMapping parentMapping = ((YAMLKeyValue) yamlKeyValue).getParentMapping();
        if(parentMapping == null) {
            return;
        }

        PsiElement parent = parentMapping.getParent();
        if(!(parent instanceof YAMLKeyValue)) {
            return;
        }

        String keyText = ((YAMLKeyValue) parent).getKeyText();

        if(!"defaults".equals(keyText)) {
            return;
        }

        YAMLMapping parentMapping1 = ((YAMLKeyValue) parent).getParentMapping();
        if(parentMapping1 == null) {
            return;
        }

        PsiElement parent1 = parentMapping1.getParent();
        if(!(parent1 instanceof YAMLKeyValue)) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.FORM_TYPE_LINE_MARKER).
            setTargets(classesInterface).
            setTooltipText("Navigate to form");

        results.add(builder.createLineMarkerInfo(parent1.getFirstChild()));
    }
}
