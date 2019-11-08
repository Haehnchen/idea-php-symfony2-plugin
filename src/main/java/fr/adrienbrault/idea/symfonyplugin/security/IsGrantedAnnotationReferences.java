package fr.adrienbrault.idea.symfonyplugin.security;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.security.utils.VoterUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Support: @IsGranted("VOTER_ATTRIBUTE")
 */
public class IsGrantedAnnotationReferences implements PhpAnnotationReferenceProvider {
    private static String IS_GRANTED_CLASS = "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\IsGranted";

    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter annotationPropertyParameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {
        Project project = annotationPropertyParameter.getProject();
        if(!Symfony2ProjectComponent.isEnabled(project) || !(annotationPropertyParameter.getElement() instanceof StringLiteralExpression) || !PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), IS_GRANTED_CLASS)) {
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
