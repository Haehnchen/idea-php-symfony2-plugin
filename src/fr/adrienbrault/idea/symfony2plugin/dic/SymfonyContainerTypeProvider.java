package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.PhpTypeCache;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeCacheIndex;
import org.jetbrains.annotations.Nullable;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class SymfonyContainerTypeProvider implements PhpTypeProvider {

    private Symfony2InterfacesUtil interfacesUtil;

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled || !Settings.getInstance(e.getProject()).symfonyContainerTypeProvider) {
            return null;
        }

        // container calls are only on "get" methods
        if(!PhpElementsUtil.isMethodWithFirstString(e, "get")) {
            return null;
        }

        PhpTypeCache cache = PhpTypeCacheIndex.getInstance(e.getProject(), SymfonyContainerTypeProvider.class);
        if(cache.hasSignature(e)) {
            return cache.getSignatureCache(e);
        }

        boolean isContainerGetCall = getInterfacesUtil().isContainerGetCall(e);

        if (!isContainerGetCall) {
            return cache.addSignatureCache(e, null);
        }

        String serviceId = Symfony2InterfacesUtil.getFirstArgumentStringValue((MethodReference) e);
        if (null == serviceId) {
            return cache.addSignatureCache(e, null);
        }

        Symfony2ProjectComponent symfony2ProjectComponent = e.getProject().getComponent(Symfony2ProjectComponent.class);
        ServiceMap serviceMap = symfony2ProjectComponent.getServicesMap();
        if (null == serviceMap) {
            return cache.addSignatureCache(e, null);
        }
        String serviceClass = serviceMap.getMap().get(serviceId);

        if (null == serviceClass) {
            return cache.addSignatureCache(e, null);
        }

        return cache.addSignatureCache(e, new PhpType().add(serviceClass));
    }

    private Symfony2InterfacesUtil getInterfacesUtil() {
        if (null == interfacesUtil) {
            interfacesUtil = new Symfony2InterfacesUtil();
        }

        return interfacesUtil;
    }

}
