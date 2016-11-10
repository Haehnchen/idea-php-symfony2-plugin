package fr.adrienbrault.idea.symfony2plugin.codeInsight.utils;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlText;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.ArrayList;
import java.util.Collection;

public class GotoCompletionUtil {

    private static final ExtensionPointName<GotoCompletionRegistrar> EXTENSIONS = new ExtensionPointName<>("fr.adrienbrault.idea.symfony2plugin.extension.GotoCompletionRegistrar");

    public static Collection<GotoCompletionContributor> getContributors(final PsiElement psiElement) {

        final Collection<GotoCompletionContributor> contributors = new ArrayList<>();

        GotoCompletionRegistrarParameter registrar = (pattern, contributor) -> {
            if(pattern.accepts(psiElement)) {
                contributors.add(contributor);
            }
        };

        for(GotoCompletionRegistrar register: EXTENSIONS.getExtensions()) {
            register.register(registrar);
        }

        return contributors;
    }

    @Nullable
    public static String getTextValueForElement(@NotNull PsiElement psiElement) {
        PsiElement parent = psiElement.getParent();

        String value = null;
        if(parent instanceof StringLiteralExpression) {
            value = ((StringLiteralExpression) parent).getContents();
        } else if(parent instanceof XmlAttributeValue) {
            // <foo attr="FOO"/>
            value = ((XmlAttributeValue) parent).getValue();
        } else if(parent instanceof XmlText) {
            // <foo>FOO</foo>
            value = ((XmlText) parent).getValue();
        } else if(parent instanceof YAMLScalar) {
            // foo: foo, foo: 'foo', foo: "foo"
            value = ((YAMLScalar) parent).getTextValue();
        }

        if(StringUtils.isBlank(value)) {
            return null;
        }

        return value;
    }

    /**
     * <foo attr="FOO"/>
     */
    @Nullable
    public static String getXmlAttributeValue(@NotNull PsiElement psiElement) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof XmlAttributeValue)) {
            return null;
        }

        final String value = ((XmlAttributeValue) parent).getValue();
        if(StringUtils.isBlank(value)) {
            return null;
        }

        return value;
    }

    @Nullable
    public static String getStringLiteralValue(@NotNull PsiElement psiElement) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof StringLiteralExpression)) {
            return null;
        }

        String contents = ((StringLiteralExpression) parent).getContents();
        if(StringUtils.isBlank(contents)) {
            return null;
        }

        return contents;
    }
}
