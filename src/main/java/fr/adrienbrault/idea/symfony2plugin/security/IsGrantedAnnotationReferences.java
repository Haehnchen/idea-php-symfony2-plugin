package fr.adrienbrault.idea.symfony2plugin.security;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.security.utils.VoterUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Support: @IsGranted("VOTER_ATTRIBUTE")
 */
public class IsGrantedAnnotationReferences implements PhpAnnotationReferenceProvider {
    private static final String[] IS_GRANTED_CLASS = new String[] {
        "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\IsGranted",
        "Symfony\\Component\\Security\\Http\\Attribute\\IsGranted" // Symfony 6.2
    };

    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter annotationPropertyParameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {
        Project project = annotationPropertyParameter.getProject();
        if(!Symfony2ProjectComponent.isEnabled(project) || !(annotationPropertyParameter.getElement() instanceof StringLiteralExpression) || Arrays.stream(IS_GRANTED_CLASS).noneMatch(s -> PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), s))) {
            return new PsiReference[0];
        }

        if(annotationPropertyParameter.getType() != AnnotationPropertyParameter.Type.DEFAULT) {
            return new PsiReference[0];
        }

        return new PsiReference[] {
            new VoterReference(((StringLiteralExpression) annotationPropertyParameter.getElement()))
        };
    }

    private static class VoterReference extends PsiPolyVariantReferenceBase<PsiElement> {
        @NotNull
        private final StringLiteralExpression element;

        VoterReference(@NotNull StringLiteralExpression element) {
            super(element);
            this.element = element;
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            VoterUtil.LookupElementPairConsumer consumer = new VoterUtil.LookupElementPairConsumer();
            VoterUtil.visitAttribute(this.myElement.getProject(), consumer);
            return consumer.getLookupElements().toArray();
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean incompleteCode) {
            VoterUtil.TargetPairConsumer voterConsumer = new VoterUtil.TargetPairConsumer(element.getContents());
            VoterUtil.visitAttribute(this.myElement.getProject(), voterConsumer);
            return PsiElementResolveResult.createResults(voterConsumer.getValues());
        }
    }
}
