<?php

namespace Symfony\UX\Css\Twig
{
    use Twig\Extension\AbstractExtension;
    use Twig\TwigFunction;

    final class CssExtension extends AbstractExtension
    {
        public function getFunctions(): array
        {
            return [
                new TwigFunction('css', [CssRuntime::class, 'css']),
            ];
        }
    }

    /**
     * @psalm-import-type CssStyles from \Symfony\UX\Css\CssReference
     */
    final class CssRuntime
    {
        /**
         * @param CssStyles|null $styles
         */
        public function css(?array $styles): string
        {
            return '';
        }
    }
}
