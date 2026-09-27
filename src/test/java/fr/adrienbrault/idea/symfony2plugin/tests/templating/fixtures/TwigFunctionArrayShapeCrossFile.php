<?php

namespace App\Twig\Runtime;

use App\Twig\ButtonExtension;

/**
 * @psalm-import-type ButtonOptions from ButtonExtension
 */
final class ButtonCrossFileRuntime
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
 * @psalm-import-type MenuOptions from ButtonExtension
 */
final class MenuRuntime
{
    /**
     * @param MenuOptions $options
     */
    public function render(array $options): string
    {
        return '';
    }
}
