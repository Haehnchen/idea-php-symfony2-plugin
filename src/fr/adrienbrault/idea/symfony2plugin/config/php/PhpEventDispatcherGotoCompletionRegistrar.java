package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpEventDispatcherGotoCompletionRegistrar implements GotoCompletionRegistrar {

    /**
     *
     * \Symfony\Component\EventDispatcher\EventSubscriberInterface::getSubscribedEvents
     *
     * return array(
     *  'pre.foo' => array('preFoo', 10),
     *  'post.foo' => array('postFoo'),
     * ');
     *
     */
    public void register(GotoCompletionRegistrarParameter registrar) {

        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), psiElement -> {
            PsiElement parent = psiElement.getParent();
            if(!(parent instanceof StringLiteralExpression)) {
                return null;
            }

            PsiElement arrayValue = parent.getParent();
            if(arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                PhpReturn phpReturn = PsiTreeUtil.getParentOfType(arrayValue, PhpReturn.class);
                if(phpReturn != null) {
                    Method method = PsiTreeUtil.getParentOfType(arrayValue, Method.class);
                    if(method != null) {
                        String name = method.getName();
                        if("getSubscribedEvents".equals(name)) {
                            PhpClass containingClass = method.getContainingClass();
                            if(containingClass != null && PhpElementsUtil.isInstanceOf(containingClass, "\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface")) {
                                return new PhpClassPublicMethodProvider(containingClass);
                            }
                        }
                    }
                }
            }

            return null;
        });

    }


    private static class PhpClassPublicMethodProvider extends GotoCompletionProvider {

        private final PhpClass phpClass;

        public PhpClassPublicMethodProvider(PhpClass phpClass) {
            super(phpClass);
            this.phpClass = phpClass;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            Collection<LookupElement> elements = new ArrayList<>();

            for(Method method: phpClass.getMethods()) {
                if(method.getAccess().isPublic()) {
                    String name = method.getName();
                    if(!"getSubscribedEvents".equals(name) && !name.startsWith("__") && !name.startsWith("set")) {
                        elements.add(LookupElementBuilder.create(name).withIcon(method.getIcon()));
                    }
                }
            }

            return elements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {

            PsiElement parent = element.getParent();
            if(!(parent instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            String contents = ((StringLiteralExpression) parent).getContents();
            if(StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            Method method = phpClass.findMethodByName(contents);
            if(method != null) {
                return new ArrayList<>(Collections.singletonList(method));
            }

            return Collections.emptyList();
        }

    }

}


