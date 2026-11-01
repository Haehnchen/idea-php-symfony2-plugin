<?php

namespace Symfony\UX\Css;

/**
 * @psalm-type ColorsToken = 'blue.500'|'current'|'primary'|'transparent'|string
 * @psalm-type DisplayValue = '-moz-initial'|'block'|'flex'|'grid'|string
 * @psalm-type SpacingToken = '-md'|'1.5'|'4'|'md'|string
 * @psalm-type WebkitAppearanceValue = 'button'|'none'|string
 * @psalm-type ZIndexValue = 'auto'|'inherit'|int|float|string
 * @psalm-type CssStyles = array{
 *     WebkitAppearance?: WebkitAppearanceValue|array<WebkitAppearanceValue|array<array-key, mixed>>,
 *     bg?: ColorsToken|array<ColorsToken|array<array-key, mixed>>,
 *     color?: ColorsToken|array<ColorsToken|array<array-key, mixed>>,
 *     display?: DisplayValue|array<DisplayValue|array<array-key, mixed>>,
 *     p?: SpacingToken|array<SpacingToken|array<array-key, mixed>>,
 *     zIndex?: ZIndexValue|array<ZIndexValue|array<array-key, mixed>>,
 *     "2xl"?: array<string, mixed>,
 *     '@/2xl'?: array<string, mixed>,
 *     _hover?: CssStylesNested,
 * }
 * @psalm-type CssStylesNested = array{
 *     bg?: ColorsToken|array<ColorsToken|array<array-key, mixed>>,
 *     color?: ColorsToken|array<ColorsToken|array<array-key, mixed>>,
 *     _hover?: array<string, mixed>,
 * }
 */
final class CssReference
{
    public const TOKENS = [
        'colors' => [
            'current' => 'currentColor',
            'transparent' => 'rgb(0 0 0 / 0)',
            'blue.500' => '#3b82f6',
            'primary' => '#3b82f6',
        ],
        'spacing' => [
            'primary' => '1rem',
            'md' => '1rem',
            '-md' => '-1rem',
        ],
    ];
}
