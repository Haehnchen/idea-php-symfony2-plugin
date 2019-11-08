package fr.adrienbrault.idea.symfonyplugin.dic;

import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import fr.adrienbrault.idea.symfonyplugin.util.ParameterBag;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodReferenceBag {

    final private ParameterList parameterList;
    final private MethodReference methodReference;
    final private ParameterBag parameterBag;

    public MethodReferenceBag(ParameterList parameterList, MethodReference methodReference, ParameterBag parameterBag) {
        this.parameterList = parameterList;
        this.methodReference = methodReference;
        this.parameterBag = parameterBag;
    }

    public ParameterList getParameterList() {
        return parameterList;
    }

    public MethodReference getMethodReference() {
        return methodReference;
    }

    public ParameterBag getParameterBag() {
        return parameterBag;
    }

}