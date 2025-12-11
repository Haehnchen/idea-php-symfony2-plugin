package fr.adrienbrault.idea.symfony2plugin.completion.command;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Symfony\Component\Console\Output\OutputInterface
 *
 * $input->getOption('<caret>'); $input->hasOption('<caret>')
 * $input->getArgument('<caret>'); $input->hasArgument('<caret>')
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpCommandGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {

        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), psiElement -> {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            for(String s : new String[] {"Option", "Argument"}) {
                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
                        .withSignature("Symfony\\Component\\Console\\Input\\InputInterface", "get" + s)
                        .withSignature("Symfony\\Component\\Console\\Input\\InputInterface", "has" + s)
                        .match();

                if(methodMatchParameter != null) {
                    PhpClass phpClass = PsiTreeUtil.getParentOfType(methodMatchParameter.getMethodReference(), PhpClass.class);
                    if(phpClass != null) {
                        return new CommandGotoCompletionProvider(phpClass, s);
                    }

                }
            }

            return null;
        });

    }

    /**
     * Collect "Options" and "Arguments" on "configure" function
     */
    private static class CommandGotoCompletionProvider extends GotoCompletionProvider {

        final private PhpClass phpClass;
        final private String addMethod;

        public CommandGotoCompletionProvider(PhpClass phpClass, String addMethod) {
            super(phpClass);
            this.phpClass = phpClass;
            this.addMethod = "add" + addMethod;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            Collection<LookupElement> elements = new ArrayList<>();

            if("addOption".equals(addMethod)) {
                // For options, use Map<String, CommandOption>
                Map<String, SymfonyCommandUtil.CommandOption> targets = SymfonyCommandUtil.getCommandOptions(phpClass);

                for(SymfonyCommandUtil.CommandOption key: targets.values()) {
                    LookupElementBuilder lookup = LookupElementBuilder.create(key.name()).withIcon(Symfony2Icons.SYMFONY);

                    String description = key.description();
                    if(description != null) {

                        if(description.length() > 25) {
                            description = StringUtils.abbreviate(description, 25);
                        }

                        lookup = lookup.withTypeText(description, true);
                    }

                    if(key.defaultValue() != null) {
                        lookup = lookup.withTailText("(" + key.defaultValue() + ")", true);
                    }

                    elements.add(lookup);
                }
            } else {
                // For arguments, use Map<String, CommandArgument>
                Map<String, SymfonyCommandUtil.CommandArgument> targets = SymfonyCommandUtil.getCommandArguments(phpClass);

                for(SymfonyCommandUtil.CommandArgument key: targets.values()) {
                    LookupElementBuilder lookup = LookupElementBuilder.create(key.name()).withIcon(Symfony2Icons.SYMFONY);

                    String description = key.description();
                    if(description != null) {

                        if(description.length() > 25) {
                            description = StringUtils.abbreviate(description, 25);
                        }

                        lookup = lookup.withTypeText(description, true);
                    }

                    if(key.defaultValue() != null) {
                        lookup = lookup.withTailText("(" + key.defaultValue() + ")", true);
                    }

                    elements.add(lookup);
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

            if("addOption".equals(addMethod)) {
                Map<String, SymfonyCommandUtil.CommandOption> targets = SymfonyCommandUtil.getCommandOptions(phpClass);
                if(!targets.containsKey(contents)) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(targets.get(contents).target());
            } else {
                Map<String, SymfonyCommandUtil.CommandArgument> targets = SymfonyCommandUtil.getCommandArguments(phpClass);
                if(!targets.containsKey(contents)) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(targets.get(contents).target());
            }
        }
    }

}
