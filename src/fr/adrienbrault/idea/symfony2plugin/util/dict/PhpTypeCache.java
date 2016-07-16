package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTypeCache {

    private HashMap<Integer, PhpType> phpTypes = new HashMap<>();
    private HashMap<Integer, Long> lastCacheHit = new HashMap<>();
    private long lastCacheHitCheckTime;

    public PhpTypeCache() {
        this.lastCacheHitCheckTime = System.currentTimeMillis();
    }

    public boolean hasSignature(Integer signature) {
        return this.phpTypes.containsKey(signature);
    }

    public boolean hasSignature(MethodReference methodReference) {
        return this.hasSignature(methodReference.hashCode());
    }

    public boolean hasSignature(PsiElement psiElement) {
        return psiElement instanceof MethodReference && this.hasSignature((MethodReference) psiElement);
    }

    @Nullable
    public PhpType getSignatureCache(Integer signature) {
        this.lastCacheHit.put(signature, System.currentTimeMillis());
        return this.phpTypes.get(signature);
    }

    @Nullable
    public PhpType getSignatureCache(MethodReference methodReference) {
        return this.getSignatureCache(methodReference.hashCode());
    }
    @Nullable
    public PhpType getSignatureCache(PsiElement psiElement) {

        if(psiElement instanceof  MethodReference) {
            return this.getSignatureCache((MethodReference) psiElement);
        }

        return null;
    }

    public void setSignatureCache(MethodReference methodReference, PhpType phpType) {
        this.lastCacheHit.put(methodReference.hashCode(), System.currentTimeMillis());
        this.phpTypes.put(methodReference.hashCode(), phpType);
    }

    public void setSignatureCache(PsiElement psiElement, PhpType phpType) {
        if(psiElement instanceof MethodReference) {
            this.setSignatureCache((MethodReference) psiElement, phpType);
        }
    }

    public PhpType addSignatureCache(PsiElement psiElement, PhpType phpType) {
        if(psiElement instanceof MethodReference) {
            this.setSignatureCache((MethodReference) psiElement, phpType);
        }

        return phpType;
    }

    public HashMap<Integer, PhpType> getCachedTypes() {
        return this.phpTypes;
    }

    public long getLastCacheHitCheckTime() {
        return this.lastCacheHitCheckTime;
    }

    public void removeExpiredTypes(Long expiredTime) {

        for(Map.Entry<Integer, Long> entrySet: this.lastCacheHit.entrySet()) {
            if(entrySet.getValue() < expiredTime) {
                this.phpTypes.remove(entrySet.getKey());
            }
        }

        this.lastCacheHitCheckTime = System.currentTimeMillis();
    }

}
