<?php

namespace Twig
{
    class Environment
    {
    }

    final class TwigFunction
    {
        public function __construct(string $name, $callable = null, array $options = [])
        {
        }
    }
}

namespace Twig\Extension
{
    interface ExtensionInterface
    {
    }

    abstract class AbstractExtension implements ExtensionInterface
    {
    }

    interface RuntimeExtensionInterface
    {
    }
}

namespace App\Twig
{
    use Twig\Environment;
    use Twig\Extension\AbstractExtension;
    use Twig\Extension\RuntimeExtensionInterface;
    use Twig\TwigFunction;

    /**
     * @psalm-type ButtonOptions = array{size?: 'sm'|'md'|'lg', variant?: 'primary'|'secondary'|string, icon?: IconOptions}
     * @psalm-type IconOptions = array{name?: string, position?: 'start'|'end'}
     * @psalm-type LinkOptions = array{size?: SizeValue, href: string, 'data-turbo'?: 'true'|'false'}
     * @psalm-type SizeValue = 'xs'|'xl'|string
 * @psalm-type MenuOptions = array{label?: string, children?: MenuOptions}
     */
    final class ButtonExtension extends AbstractExtension
    {
        public function getFunctions(): array
        {
            return [
                new TwigFunction('button_direct', $this->direct(...)),
                new TwigFunction('button_alias', [$this, 'alias']),
                new TwigFunction('button_imported', [ButtonRuntime::class, 'render']),
                new TwigFunction('button_env', [$this, 'withEnvironment'], ['needs_environment' => true, 'needs_context' => true]),
                new TwigFunction('button_second', [$this, 'second']),
                new TwigFunction('link', [$this, 'link']),
                new TwigFunction('badge', [BadgeRuntime::class, 'render']),
                new TwigFunction('badge_imported', [BadgeImportRuntime::class, 'render']),
                new TwigFunction('button_cross_file', [\App\Twig\Runtime\ButtonCrossFileRuntime::class, 'render']),
                new TwigFunction('menu', [\App\Twig\Runtime\MenuRuntime::class, 'render']),
            ];
        }

        /**
         * @param array{size?: 'sm'|'md'|'lg', variant?: 'primary'|'secondary'} $options
         */
        public function direct(array $options): string
        {
            return '';
        }

        /**
         * @param ButtonOptions $options
         */
        public function alias(array $options): string
        {
            return '';
        }

        /**
         * @param ButtonOptions $options
         */
        public function withEnvironment(Environment $env, array $context, array $options): string
        {
            return '';
        }

        /**
         * @param ButtonOptions $options
         */
        public function second(string $label, array $options): string
        {
            return '';
        }

        /**
         * @param LinkOptions $options
         */
        public function link(array $options): string
        {
            return '';
        }
    }

    /**
     * @psalm-import-type ButtonOptions from ButtonExtension
     */
    final class ButtonRuntime implements RuntimeExtensionInterface
    {
        /**
         * @param ButtonOptions $options
         */
        public function render(array $options): string
        {
            return '';
        }
    }

    /**
     * @phpstan-type BadgeOptions array{tone?: ToneValue, rounded?: bool}
     * @phpstan-type ToneValue 'info'|'danger'|string
     */
    final class BadgeRuntime implements RuntimeExtensionInterface
    {
        /**
         * @param BadgeOptions $options
         */
        public function render(array $options): string
        {
            return '';
        }
    }

    /**
     * @phpstan-import-type BadgeOptions from BadgeRuntime
     */
    final class BadgeImportRuntime implements RuntimeExtensionInterface
    {
        /**
         * @param BadgeOptions $options
         */
        public function render(array $options): string
        {
            return '';
        }
    }
}
