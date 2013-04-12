package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2CachedInterfacesUtil extends Symfony2InterfacesUtil {

    private Map<PsiElement, Map<String, Boolean>> isCallToCache = new HashMap<PsiElement, Map<String, Boolean>>();
    private Map<String, Boolean> isInstanceOfCache = new HashMap<String, Boolean>();

    protected boolean isCallTo(PsiElement e, Method[] expectedMethods) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Method method : Arrays.asList(expectedMethods)) {
            stringBuilder.append(method.getFQN()).append("_");
        }

        Map<String, Boolean> elementCache = isCallToCache.get(e);
        Boolean cachedValue = null;
        if (null != elementCache) {
            cachedValue = elementCache.get(stringBuilder.toString());
        }

        if (null != cachedValue) {
            return cachedValue;
        }

        boolean result = super.isCallTo(e, expectedMethods);

        if (null == elementCache) {
            elementCache = new HashMap<String, Boolean>();
            isCallToCache.put(e, elementCache);
        }

        elementCache.put(stringBuilder.toString(), result);

        return result;
    }

    protected boolean isInstanceOf(PhpClass subjectClass, PhpClass expectedClass) {
        String cacheKey = subjectClass.getFQN() + "@@@" + expectedClass.getFQN();
        Boolean cachedValue = isInstanceOfCache.get(cacheKey);

        if (null != cachedValue) {
            return cachedValue;
        }

        boolean result = super.isInstanceOf(subjectClass, expectedClass);
        isInstanceOfCache.put(cacheKey, true);

        return result;
    }

}
