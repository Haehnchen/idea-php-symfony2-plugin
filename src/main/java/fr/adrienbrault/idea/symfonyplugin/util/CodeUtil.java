package fr.adrienbrault.idea.symfonyplugin.util;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CodeUtil {

    /**
     *
     * @param phpClass PhpClass
     * @param methodName The method
     * @return last int position of method psi element
     */
    public static int getMethodInsertPosition(PhpClass phpClass, String methodName) {

        // empty class
        Method[] ownMethods = phpClass.getOwnMethods();
        if(ownMethods.length == 0) {
            return phpClass.getTextRange().getEndOffset() - 1;
        }

        // collection method names and sort them, to get method matching before
        List<String> methods = new ArrayList<>();
        methods.add(methodName);
        for (Method method: ownMethods) {
            methods.add(method.getName());
        }

        Collections.sort(methods);

        // first method
        int post = methods.indexOf(methodName);
        if(post == 0) {
            return phpClass.getTextRange().getEndOffset() - 1;
        }

        // find method after we should insert method
        Method method = phpClass.findOwnMethodByName(methods.get(post - 1));
        if(method == null) {
            return -1;
        }

        return method.getTextRange().getEndOffset();
    }

}
