package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class SymfonyContainerTypeProvider implements PhpTypeProvider {

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb()) {
            return null;
        }

        if (!ContainerGetHelper.isContainerGetCall(e)) {
            return null;
        }

        String serviceId = ContainerGetHelper.getServiceId((MethodReference) e);
        if (null == serviceId) {
            return null;
        }

        Symfony2ProjectComponent symfony2ProjectComponent = e.getProject().getComponent(Symfony2ProjectComponent.class);
        ServiceMap serviceMap = symfony2ProjectComponent.getServicesMap();
        if (null == serviceMap) {
            return null;
        }
        String serviceClass = serviceMap.getMap().get(serviceId);

        if (null == serviceClass) {
            return null;
        }

        return new PhpType().add(serviceClass);
    }

}
