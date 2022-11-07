package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.*;

public class YamlReferenceContributor extends PsiReferenceContributor {
    private static final String TAG_PHP_CONST = "!php/const";

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar.class)
                .withText(StandardPatterns.string()
                    .contains(TAG_PHP_CONST)
                ),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                    if (!Symfony2ProjectComponent.isEnabled(element)) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    var scalar = (YAMLScalar) element;
                    if (scalar.getTextValue().isEmpty()) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    return new PsiReference[]{
                        new ConstantYamlReference(scalar)
                    };
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // services:
        //     app.service.foo:
        //         arguments:
        //             - '@app.service.bar<caret>'
        registrar.registerReferenceProvider(
            PlatformPatterns
                .psiElement(YAMLScalar.class)
                .withParent(
                    PlatformPatterns
                        .psiElement(YAMLSequenceItem.class)
                        .withParent(
                            PlatformPatterns
                                .psiElement(YAMLSequence.class)
                                .withParent(
                                    PlatformPatterns.psiElement(YAMLKeyValue.class).withName("arguments")
                                )
                        )
                ),
            new YAMLScalarServiceReferenceProvider()
        );

        // services:
        //     app.service.foo:
        //         arguments:
        //             $bar: '@app.service.bar'

        // services:
        //     app.service.foo:
        //         properties:
        //             bar: '@app.service.bar'

        registrar.registerReferenceProvider(
            PlatformPatterns
                .psiElement(YAMLScalar.class)
                .withParent(
                    PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withParent(
                            PlatformPatterns
                                .psiElement(YAMLMapping.class)
                                .withParent(
                                    PlatformPatterns
                                        .psiElement(YAMLKeyValue.class)
                                        .withName("arguments", "properties")
                                )
                        )
                ),
            new YAMLScalarServiceReferenceProvider()
        );

        // services:
        //     app.service.foo:
        //         alias: app.service.bar

        // services:
        //     app.service.foo_decorator:
        //         decorates: app.service.foo

        // services:
        //     app.service.foo:
        //         parent: app.service.foo_parent

        registrar.registerReferenceProvider(
            PlatformPatterns
                .psiElement(YAMLScalar.class)
                .withParent(
                    PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withName("alias", "decorates", "parent")
                ),
            new YAMLScalarServiceReferenceProvider(false)
        );

        // services:
        //     app.service.foo:
        //         configurator: @app.service.foo_configurator

        // services:
        //     app.service.foo:
        //         factory: @app.service.foo_factory

        registrar.registerReferenceProvider(
            PlatformPatterns
                .psiElement(YAMLScalar.class)
                .withParent(
                    PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withName("configurator", "factory")
                ),
            new YAMLScalarServiceReferenceProvider()
        );

        // services:
        //     app.service.foo:
        //         factory: ['@app.service.foo_factory', 'create']

        // services:
        //     app.service.foo:
        //         configurator: ['@app.service.foo_configurator', 'configure']

        registrar.registerReferenceProvider(
            PlatformPatterns
                .psiElement(YAMLScalar.class)
                .withParent(
                    PlatformPatterns
                        .psiElement(YAMLSequenceItem.class)
                        .with(new PatternCondition<>("is first sequence item") {
                            @Override
                            public boolean accepts(@NotNull YAMLSequenceItem element, ProcessingContext context) {
                                return element.getItemIndex() == 0;
                            }
                        })
                        .withParent(
                            PlatformPatterns
                                .psiElement(YAMLSequence.class)
                                .withParent(
                                    PlatformPatterns.psiElement(YAMLKeyValue.class).withName("factory", "configurator")
                                )
                        )
                ),
            new YAMLScalarServiceReferenceProvider()
        );

        // services:
        //     app.service.foo: '@app.service.bar'
        registrar.registerReferenceProvider(
            PlatformPatterns
                .psiElement(YAMLScalar.class)
                .withParent(
                    PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withParent(
                            PlatformPatterns
                                .psiElement(YAMLMapping.class)
                                .withParent(
                                    PlatformPatterns
                                        .psiElement(YAMLKeyValue.class)
                                        .withName("services")
                                )
                        )
                ),
            new YAMLScalarServiceReferenceProvider()
        );

        // services:
        //     app.service.foo:
        //         calls:
        //             - setBar: [ '@app.service.bar' ]
        registrar.registerReferenceProvider(
            PlatformPatterns
                .psiElement(YAMLScalar.class)
                .withParent(
                    PlatformPatterns
                        .psiElement(YAMLSequenceItem.class)
                        .withParent(
                            PlatformPatterns
                                .psiElement(YAMLSequence.class)
                                .withParent(
                                    PlatformPatterns
                                        .psiElement(YAMLKeyValue.class)
                                        .withParent(
                                            PlatformPatterns
                                                .psiElement(YAMLMapping.class)
                                                .withParent(
                                                    PlatformPatterns
                                                        .psiElement(YAMLSequenceItem.class)
                                                        .withParent(
                                                            PlatformPatterns
                                                                .psiElement(YAMLSequence.class)
                                                                .withParent(
                                                                    PlatformPatterns
                                                                        .psiElement(YAMLKeyValue.class)
                                                                        .withName("calls")
                                                                )
                                                        )
                                                )
                                        )

                                )
                        )
                ),
            new YAMLScalarServiceReferenceProvider()
        );
    }

    private static class YAMLScalarServiceReferenceProvider extends PsiReferenceProvider {

        private static final String PREFIX = "@";
        private static final String ESCAPED_PREFIX = "@@";

        /**
         * Flag indicating whenever YAMLScalar value start with `@` prefix
         */
        private boolean isPrefixed = true;

        public YAMLScalarServiceReferenceProvider() {
        }

        public YAMLScalarServiceReferenceProvider(boolean isPrefixed) {
            this.isPrefixed = isPrefixed;
        }

        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            if (!Symfony2ProjectComponent.isEnabled(element)) {
                return PsiReference.EMPTY_ARRAY;
            }

            if (element instanceof YAMLScalar) {
                var serviceName = ((YAMLScalar) element).getTextValue();
                if (serviceName.isEmpty()) {
                    return PsiReference.EMPTY_ARRAY;
                }

                if (!isPrefixed) {
                    return new PsiReference[]{
                        new ServiceYamlReference(element, serviceName)
                    };
                }

                if (isValidServiceNameWithPrefix(serviceName)) {
                    var range = TextRange.from(serviceName.indexOf(PREFIX) + 1, serviceName.length() - 1);
                    if (element instanceof YAMLQuotedText) {
                        // Skip quotes
                        range = range.shiftRight(1);
                    }

                    return new PsiReference[]{
                        new ServiceYamlReference(element, range, serviceName.substring(1))
                    };
                }
            }

            return PsiReference.EMPTY_ARRAY;
        }

        private boolean isValidServiceNameWithPrefix(@NotNull String serviceName) {
            return serviceName.length() > 1 && serviceName.startsWith(PREFIX) && !serviceName.startsWith(ESCAPED_PREFIX);
        }

        public boolean acceptsTarget(@NotNull PsiElement target) {
            return Symfony2ProjectComponent.isEnabled(target);
        }
    }
}
