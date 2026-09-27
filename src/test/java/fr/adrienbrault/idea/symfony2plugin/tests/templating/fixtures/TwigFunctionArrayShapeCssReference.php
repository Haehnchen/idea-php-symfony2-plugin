<?php

namespace Symfony\UX\Css;

/**
 * @psalm-type DisplayValue = '-moz-initial'|'block'|'flex'|'grid'|string
 * @psalm-type WebkitAppearanceValue = 'button'|'none'|string
 * @psalm-type ZIndexValue = 'auto'|'inherit'|int|float|string
 * @psalm-type CssStyles = array{
 *     WebkitAppearance?: WebkitAppearanceValue|array<WebkitAppearanceValue|array<array-key, mixed>>,
 *     display?: DisplayValue|array<DisplayValue|array<array-key, mixed>>,
 *     zIndex?: ZIndexValue|array<ZIndexValue|array<array-key, mixed>>,
 *     "2xl"?: array<string, mixed>,
 *     '@/2xl'?: array<string, mixed>,
 *     _hover?: array<string, mixed>,
 * }
 */
final class CssReference
{
}
