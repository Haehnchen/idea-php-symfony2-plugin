package fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.util;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpPresentationUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.CaretTextOverlay;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay.CaretTextOverlayElement;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CaretTextOverlayUtil {

    private static final ExtensionPointName<CaretTextOverlay> EXTENSIONS = new ExtensionPointName<CaretTextOverlay>(
        "fr.adrienbrault.idea.symfony2plugin.extension.CaretTextOverlay"
    );

    public static CaretTextOverlay[] getExtensions() {
        return EXTENSIONS.getExtensions();
    }

    @Nullable
    public static CaretTextOverlayElement getCaretTextOverlayForServiceConstructor(@NotNull Project project, @NotNull String serviceName) {
        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(project, serviceName);
        if(serviceClass == null) {
            return null;
        }

        return getCaretTextOverlayForConstructor(serviceClass);
    }

    @Nullable
    private static CaretTextOverlayElement getCaretTextOverlayForConstructor(@NotNull PhpClass phpClass) {

        Method constructor = phpClass.getConstructor();
        if(constructor == null) {
            return null;
        }

        Parameter[] parameters = constructor.getParameters();
        if(parameters.length == 0) {
            return null;
        }

        return new CaretTextOverlayElement(PhpPresentationUtil.formatParameters(null, parameters).toString());
    }
}
