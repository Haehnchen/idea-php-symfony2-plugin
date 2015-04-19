package fr.adrienbrault.idea.symfony2plugin.completion.command;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public void register(GotoCompletionRegistrarParameter registrar) {

        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {

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
            }

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

            Collection<LookupElement> elements = new ArrayList<LookupElement>();

            Map<String, CommandArg> targets = getCommandConfigurationMap(phpClass, addMethod);

            for(CommandArg key: targets.values()) {
                LookupElementBuilder lookup = LookupElementBuilder.create(key.getName()).withIcon(Symfony2Icons.SYMFONY);

                String description = key.getDescription();
                if(description != null) {

                    if(description.length() > 25) {
                        description = StringUtils.abbreviate(description, 25);
                    }

                    lookup = lookup.withTypeText(description, true);
                }

                if(key.getDefaultValue() != null) {
                    lookup = lookup.withTailText("(" + key.getDefaultValue() + ")", true);
                }

                elements.add(lookup);
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

            Map<String, CommandArg> targets = getCommandConfigurationMap(phpClass, addMethod);
            if(!targets.containsKey(contents)) {
                return Collections.emptyList();
            }

            return Arrays.asList(targets.get(contents).getTarget());
        }

        @Nullable
        private String getParameterStringValue(@NotNull PsiElement[] parameters, int index) {

            if(index >= parameters.length ) {
                return null;
            }

            if(!(parameters[index] instanceof StringLiteralExpression)) {
                return null;
            }

            String contents = ((StringLiteralExpression) parameters[index]).getContents();
            if(StringUtils.isBlank(contents)) {
                return null;
            }

            return contents;
        }

        private Map<String, CommandArg> getCommandConfigurationMap(@NotNull PhpClass phpClass, final @NotNull String methodName) {

            Method configure = phpClass.findMethodByName("configure");
            if(configure == null) {
                return Collections.emptyMap();
            }

            Collection<PsiElement> psiElements = PhpElementsUtil.collectMethodElementsWithParents(configure, new CommandDefPsiElementFilter(methodName));
            if(psiElements.size() == 0) {
                return Collections.emptyMap();
            }

            Map<String, CommandArg> targets = new HashMap<String, CommandArg>();

            for (PsiElement element : psiElements) {

                if(!(element instanceof MethodReference)) {
                    continue;
                }

                /*
                  ->setDefinition(new InputArgument())
                  ->setDefinition(array(
                     new InputArgument(),
                     new InputOption(),
                  ));
                */
                if("setDefinition".equals(((MethodReference) element).getName())) {

                    Collection<NewExpression> newExpressions = PsiTreeUtil.collectElementsOfType(element, NewExpression.class);
                    for (NewExpression newExpression : newExpressions) {
                        if(methodName.equals("addOption") && PhpElementsUtil.getNewExpressionPhpClassWithInstance(newExpression, "Symfony\\Component\\Console\\Input\\InputOption") != null) {

                            // new InputOption()
                            PsiElement[] parameters = newExpression.getParameters();
                            String contents = getParameterStringValue(parameters, 0);
                            if(contents != null && StringUtils.isNotBlank(contents)) {
                                targets.put(contents, new CommandArg(parameters[0], contents, getParameterStringValue(parameters, 3), getParameterStringValue(parameters, 4)));
                            }

                        } else if(methodName.equals("addArgument") && PhpElementsUtil.getNewExpressionPhpClassWithInstance(newExpression, "Symfony\\Component\\Console\\Input\\InputArgument") != null) {

                            // new InputArgument()
                            PsiElement[] parameters = newExpression.getParameters();
                            String contents = getParameterStringValue(parameters, 0);
                            if(contents != null && StringUtils.isNotBlank(contents)) {
                                targets.put(contents, new CommandArg(parameters[0], contents, getParameterStringValue(parameters, 2), getParameterStringValue(parameters, 3)));
                            }
                        }

                    }

                } else {

                    /*
                        ->addArgument('arg3', null, 'desc')
                        ->addOption('opt1', null, null, 'desc', 'default')
                    */
                    PsiElement[] parameters = ((MethodReference) element).getParameters();
                    if(parameters.length > 0 && parameters[0] instanceof StringLiteralExpression) {
                        String contents = ((StringLiteralExpression) parameters[0]).getContents();
                        if(StringUtils.isNotBlank(contents)) {
                            if(methodName.equals("addOption")) {
                                targets.put(contents, new CommandArg(parameters[0], contents, getParameterStringValue(parameters, 3), getParameterStringValue(parameters, 4)));
                            } else if(methodName.equals("addArgument")) {
                                targets.put(contents, new CommandArg(parameters[0], contents, getParameterStringValue(parameters, 2), getParameterStringValue(parameters, 3)));
                            }
                        }
                    }
                }

            }

            return targets;
        }

        private static class CommandDefPsiElementFilter implements Processor<PsiElement> {
            private final String methodName;

            public CommandDefPsiElementFilter(String methodName) {
                this.methodName = methodName;
            }

            @Override
            public boolean process(PsiElement psiElement) {
                if(!(psiElement instanceof MethodReference)) {
                    return false;
                }

                String name = ((MethodReference) psiElement).getName();
                return methodName.equals(name) || "setDefinition".equals(name);
            }
        }
    }

    private static class CommandArg {

        @NotNull
        private final PsiElement target;
        private final String name;
        private String description;
        private String defaultValue;

        public CommandArg(@NotNull PsiElement target, @NotNull String name) {
            this.target = target;
            this.name = name;
        }

        private CommandArg(@NotNull PsiElement target, @NotNull String name, @Nullable String description, @Nullable String defaultValue) {
            this.target = target;
            this.name = name;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @Nullable
        public String getDescription() {
            return description;
        }

        @Nullable
        public String getDefaultValue() {
            return defaultValue;
        }

        @NotNull
        public PsiElement getTarget() {
            return target;
        }
    }

}
