package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class PhpStringLiteralExpressionReference extends PsiReferenceProvider {

    private ArrayList<List<String>> oneOfCall = new ArrayList<List<String>>();
    private Class referenceClass;

    public PhpStringLiteralExpressionReference(Class referenceClass) {
        this.referenceClass = referenceClass;
    }

    public PhpStringLiteralExpressionReference addCall(String className, String methodName) {
        String[] countries = {className, methodName};
        List<String> list = Arrays.asList(countries);
        this.oneOfCall.add(list);
        return this;
    }

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
        if (!(psiElement.getContext() instanceof ParameterList)) {
            return new PsiReference[0];
        }
        ParameterList parameterList = (ParameterList) psiElement.getContext();

        if (parameterList == null || !(parameterList.getContext() instanceof MethodReference)) {
            return new PsiReference[0];
        }
        MethodReference method = (MethodReference) parameterList.getContext();

        Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
        for(List<String> list: this.oneOfCall) {
            String[] callItem = list.toArray(new String[list.size()]);
            if (symfony2InterfacesUtil.isCallTo(method, callItem[0], callItem[1])) {
                return this.getPsiReferenceBase(psiElement);
            }
        }

        return new PsiReference[0];
    }

    private PsiReference[] getPsiReferenceBase(PsiElement psiElement) {

        try {
            PsiReferenceBase referenceClassInstance = (PsiReferenceBase) this.referenceClass.getDeclaredConstructor(StringLiteralExpression.class).newInstance((StringLiteralExpression) psiElement);
            return new PsiReference[]{  referenceClassInstance };
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (InvocationTargetException ignored) {
        } catch (NoSuchMethodException ignored) {
        }

        return new PsiReference[0];
    }
}