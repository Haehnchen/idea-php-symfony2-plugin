<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use Symfony\Bridge\Twig\Command\DebugCommand;
use Symfony\Bundle\TwigBundle\Command\LintCommand;
use Symfony\Component\DependencyInjection\Container;

return static function (ContainerConfigurator $container) {
    $container->services()
        ->set('twig.command.debug', DebugCommand::class)
            ->tag('console.command')
            ->public()
        ->alias('twig.command.debug_alias', 'foo_alias')
        ->set('twig.service.decorated', LintCommand::class)
            ->decorate('twig.command.debug', 'twig.service.decorated.inner_custom')
        ->set('twig.service.with_parent', LintCommand::class)
            ->parent('twig.command.debug')
            ->lazy()
            ->autowire()
    ;
};