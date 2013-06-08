package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.PhpTypeCache;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeCacheIndex;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectRepositoryTypeProvider implements PhpTypeProvider {


    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb() || !Settings.getInstance(e.getProject()).pluginEnabled || !Settings.getInstance(e.getProject()).objectRepositoryTypeProvider) {
            return null;
        }

        if(!PhpElementsUtil.isMethodWithFirstString(e, "getRepository")) {
            return null;
        }

        PhpTypeCache cache = PhpTypeCacheIndex.getInstance(e.getProject(), ObjectRepositoryTypeProvider.class);
        if(cache.hasSignature(e)) {
            return cache.getSignatureCache(e);
        }

        Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
        if (!interfacesUtil.isGetRepositoryCall(e)) {
            return cache.addSignatureCache(e, null);
        }

        String repositoryName = Symfony2InterfacesUtil.getFirstArgumentStringValue((MethodReference) e);
        if (null == repositoryName) {
            return cache.addSignatureCache(e, null);
        }

        // @TODO: parse xml or yml for repositoryClass?
        PhpClass phpClass = EntityHelper.resolveShortcutName(e.getProject(), repositoryName + "Repository");

        if(phpClass == null) {
            return cache.addSignatureCache(e, null);
        }

        return cache.addSignatureCache(e, new PhpType().add(phpClass));
    }

}
