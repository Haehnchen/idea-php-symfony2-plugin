package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dbal;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineDbalQbGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {

        // table name completion on method eg:
        // Doctrine\DBAL\Connection::insert
        // Doctrine\DBAL\Query\QueryBuilder::update
        registrar.register(PhpElementsUtil.getParameterInsideMethodReferencePattern(), psiElement -> {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            if (!isTableNameRegistrar(context)) {
                return null;
            }

            return new DbalTableGotoCompletionProvider(context);
        });

        // simple flat field names eg:
        // Doctrine\DBAL\Connection::update('foo', ['<caret>'])
        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), psiElement -> {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(context, 1)
                .withSignature("\\Doctrine\\DBAL\\Connection", "insert")
                .withSignature("\\Doctrine\\DBAL\\Connection", "update")
                .match();

            if (methodMatchParameter == null) {
                return null;
            }

            PsiElement[] parameters = methodMatchParameter.getParameters();
            if(parameters.length < 2) {
                return null;
            }

            String stringValue = PhpElementsUtil.getStringValue(parameters[0]);
            if(StringUtils.isBlank(stringValue)) {
                return null;
            }

            return new DbalFieldGotoCompletionProvider(context, stringValue);
        });

        // simple word alias completion
        // join('foo', 'foo', 'bar')
        registrar.register(PhpElementsUtil.getParameterInsideMethodReferencePattern(), psiElement -> {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 2)
                .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "innerJoin")
                .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "leftJoin")
                .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "join")
                .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "rightJoin")
                .match();

            if (methodMatchParameter == null) {
                return null;
            }

            PsiElement[] parameters = methodMatchParameter.getParameters();
            if(parameters.length < 2) {
                return null;
            }

            String stringValue = PhpElementsUtil.getStringValue(parameters[1]);
            if(StringUtils.isBlank(stringValue)) {
                return null;
            }

            return new MyDbalAliasGotoCompletionProvider(context, stringValue, PhpElementsUtil.getStringValue(parameters[0]));
        });

    }

    private static class MyDbalAliasGotoCompletionProvider extends GotoCompletionProvider {

        @NotNull
        private final String value;

        @Nullable
        private final String fromAlias;

        public MyDbalAliasGotoCompletionProvider(PsiElement psiElement, @NotNull String value, @Nullable String fromAlias) {
            super(psiElement);
            this.value = value;
            this.fromAlias = fromAlias;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            Set<String> aliasSet = new HashSet<>();
            aliasSet.add(value);
            aliasSet.add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(value, true));
            String underscore = fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(value);
            aliasSet.add(underscore);

            if(!value.isEmpty()) {
                aliasSet.add(value.substring(0, 1));
            }

            if(underscore.contains("_")) {
                String[] split = underscore.split("_");
                if(split.length > 1) {
                    aliasSet.add(split[0].substring(0, 1) + split[1].substring(0, 1));
                }

                List<String> i = new ArrayList<>();
                for (String s : split) {
                    i.add(s.substring(0, 1));
                }
                aliasSet.add(StringUtils.join(i, ""));
            }

            if(StringUtils.isNotBlank(this.fromAlias)) {
                aliasSet.add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(this.fromAlias + "_" + value, true));
                aliasSet.add(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(this.fromAlias + "_" + value));
            }

            Collection<LookupElement> lookupElements = new ArrayList<>();
            for (String s : aliasSet) {
                lookupElements.add(LookupElementBuilder.create(s).withIcon(Symfony2Icons.DOCTRINE));
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            return Collections.emptyList();
        }
    }

    private boolean isTableNameRegistrar(PsiElement context) {

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "update")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "insert")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "from")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "delete")
            .withSignature("Doctrine\\DBAL\\Connection", "insert")
            .withSignature("Doctrine\\DBAL\\Connection", "update")
            .match();

        if(methodMatchParameter != null) {
            return true;
        }

        methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 1)
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "innerJoin")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "leftJoin")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "join")
            .withSignature("Doctrine\\DBAL\\Query\\QueryBuilder", "rightJoin")
            .match();

        return methodMatchParameter != null;

    }

    private static class DbalTableGotoCompletionProvider extends GotoCompletionProvider {

        public DbalTableGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> elements = new ArrayList<>();

            for (Pair<String, PsiElement> pair : DoctrineMetadataUtil.getTables(getProject())) {
                elements.add(LookupElementBuilder.create(pair.getFirst()).withIcon(Symfony2Icons.DOCTRINE));
            }

            return elements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if(contents == null) {
                return Collections.emptyList();
            }

            Collection<PsiElement> psiElements = new ArrayList<>();

            for (Pair<String, PsiElement> pair : DoctrineMetadataUtil.getTables(getProject())) {
                if(!contents.equals(pair.getFirst())) {
                    continue;
                }

                PsiElement second = pair.getSecond();
                if(second == null) {
                    continue;
                }

                psiElements.add(second);
            }

            return psiElements;
        }
    }

    private static class DbalFieldGotoCompletionProvider extends GotoCompletionProvider {

        private final String stringValue;

        public DbalFieldGotoCompletionProvider(PsiElement element, String stringValue) {
            super(element);
            this.stringValue = stringValue;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            DoctrineMetadataModel model = DoctrineMetadataUtil.getMetadataByTable(getProject(), this.stringValue);
            if(model == null) {
                return Collections.emptyList();
            }

            Collection<LookupElement> elements = new ArrayList<>();
            for (DoctrineModelField field : model.getFields()) {
                String column = field.getColumn();

                // use "column" else fallback to field name
                if(column != null && StringUtils.isNotBlank(column)) {
                    elements.add(LookupElementBuilder.create(column).withIcon(Symfony2Icons.DOCTRINE));
                } else {
                    String name = field.getName();
                    if(StringUtils.isNotBlank(name)) {
                        elements.add(LookupElementBuilder.create(name).withIcon(Symfony2Icons.DOCTRINE));
                    }
                }
            }

            return elements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if(contents == null) {
                return Collections.emptyList();
            }

            DoctrineMetadataModel model = DoctrineMetadataUtil.getMetadataByTable(getProject(), this.stringValue);
            if(model == null) {
                return Collections.emptyList();
            }

            Collection<PsiElement> elements = new ArrayList<>();
            for (DoctrineModelField field : model.getFields()) {
                if(contents.equals(field.getColumn()) || contents.equals(field.getName())) {
                    elements.addAll(field.getTargets());
                }
            }

            return elements;
        }
    }
}
