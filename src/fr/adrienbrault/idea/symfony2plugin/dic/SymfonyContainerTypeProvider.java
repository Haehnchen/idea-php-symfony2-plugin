package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.DumbService;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.Symfony2CachedInterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class SymfonyContainerTypeProvider implements PhpTypeProvider {

    private Symfony2CachedInterfacesUtil cachedInterfacesUtil;

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb()) {
            return null;
        }

        // filter out method calls without parameter
        // $this->get('service_name')
        if(!PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE).withChild(
                PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST).withFirstChild(
                        PlatformPatterns.psiElement(PhpElementTypes.STRING))).accepts(e)) {

            return null;
        }

        // container calls are only on "get" methods
        // cant we move it up to PlatformPatterns? withName condition dont looks working
        String methodRefName = ((MethodReference) e).getName();
        if(methodRefName == null || !methodRefName.equals("get")) {
            return null;
        }

        boolean shouldUnsetCachedInterfaceUtil = false;

        if (null == cachedInterfacesUtil) {
            cachedInterfacesUtil = new Symfony2CachedInterfacesUtil();
            shouldUnsetCachedInterfaceUtil = true;
        }

        boolean isContainerGetCall = cachedInterfacesUtil.isContainerGetCall(e);

        if (shouldUnsetCachedInterfaceUtil) {
            cachedInterfacesUtil = null;
        }

        if (!isContainerGetCall) {
            return null;
        }

        String serviceId = Symfony2InterfacesUtil.getFirstArgumentStringValue((MethodReference) e);
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
