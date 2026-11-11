<?php

namespace App\Twig\Cva
{
    use Twig\Environment;
    use Twig\Extension\AbstractExtension;
    use Twig\TwigFunction;

    /**
     * @psalm-type VariantMap = array<string, array{size?: 'sm'|'lg'}>
     * @psalm-type ItemOptions = array{css?: array{color?: 'red'|'blue'}}
     * @psalm-type InlineIcon = array{name?: string, position?: 'start'|'end'}
     * @psalm-import-type IconOptions from \App\Twig\ButtonExtension
     */
    final class CvaFixtureExtension extends AbstractExtension
    {
        public function getFunctions(): array
        {
            return [
                new TwigFunction('button_named', [$this, 'named'], ['needs_environment' => true]),
                new TwigFunction('map_value', [$this, 'mapValue']),
                new TwigFunction('map_alias', [$this, 'mapAlias']),
                new TwigFunction('list_item', [$this, 'listItem']),
                new TwigFunction('list_alias', [$this, 'listAlias']),
                new TwigFunction('cva', [CvaRuntime::class, 'cva']),
                new TwigFunction('html_cva', [$this, 'htmlCva']),
                new TwigFunction('not_cva', [$this, 'notCva']),
                new TwigFunction('array_single', [$this, 'arraySingle']),
                new TwigFunction('iterable_value', [$this, 'iterableValue']),
                new TwigFunction('non_empty_list', [$this, 'nonEmptyList']),
                new TwigFunction('inline_alias', [$this, 'inlineAlias']),
                new TwigFunction('inline_imported', [$this, 'inlineImported']),
                new TwigFunction('inline_phpstan', [PhpStanInlineRuntime::class, 'render']),
            ];
        }

        /**
         * @param array<array{size?: 'sm'|'lg'}> $items
         */
        public function arraySingle(array $items): string
        {
            return '';
        }

        /**
         * @param iterable<string, array{size?: 'sm'|'lg'}> $items
         */
        public function iterableValue(iterable $items): string
        {
            return '';
        }

        /**
         * @param non-empty-list<array{size?: 'sm'|'lg'}> $items
         */
        public function nonEmptyList(array $items): string
        {
            return '';
        }

        /**
         * @param array{icon?: InlineIcon} $options
         */
        public function inlineAlias(array $options): string
        {
            return '';
        }

        /**
         * @param array{icon?: IconOptions} $options
         */
        public function inlineImported(array $options): string
        {
            return '';
        }

        /**
         * @param array{size?: 'sm'|'lg', tone?: string} $options
         */
        public function named(Environment $env, string $label = '', array $options = []): string
        {
            return '';
        }

        /**
         * @param array<string, array{size?: 'sm'|'lg'}> $variants
         */
        public function mapValue(array $variants): string
        {
            return '';
        }

        /**
         * @param VariantMap $variants
         */
        public function mapAlias(array $variants): string
        {
            return '';
        }

        /**
         * @param list<array{css?: array{color?: 'red'|'blue'}}> $items
         */
        public function listItem(array $items): string
        {
            return '';
        }

        /**
         * @param ItemOptions[] $items
         */
        public function listAlias(array $items): string
        {
            return '';
        }

        /**
         * @param list<array{class?: string|list<string>}> $compoundVariants
         */
        public function htmlCva(array|string $base = [], array $variants = [], array $compoundVariants = [], array $defaultVariant = []): string
        {
            return '';
        }

        /**
         * @param array<string, array<string, string>> $variants
         * @param array<string, string> $defaultVariants
         */
        public function notCva(array $variants = [], array $defaultVariants = []): string
        {
            return '';
        }
    }

    /**
     * @phpstan-type StanIcon array{name?: string, position?: 'start'|'end'}
     */
    final class PhpStanInlineRuntime
    {
        /**
         * @param array{icon?: StanIcon} $options
         */
        public function render(array $options): string
        {
            return '';
        }
    }

    /**
     * @psalm-import-type CssStyles from \Symfony\UX\Css\CssReference
     */
    final class CvaRuntime
    {
        /**
         * @param CssStyles                                $base
         * @param array<string, array<string, CssStyles>>  $variants
         * @param list<array{css?: CssStyles}>             $compoundVariants
         * @param array<string, string|int|float|bool>     $defaultVariants
         */
        public function cva(array $base = [], array $variants = [], array $compoundVariants = [], array $defaultVariants = []): string
        {
            return '';
        }
    }
}
