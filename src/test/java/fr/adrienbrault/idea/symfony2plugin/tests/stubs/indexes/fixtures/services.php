<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use Symfony\Bridge\Twig\Command\DebugCommand;
use Symfony\Bundle\TwigBundle\Command\LintCommand;
use Symfony\Component\DependencyInjection\Container;

return static function (ContainerConfigurator $container) {
    $container->services()
        ->set('twig.command.debug', DebugCommand::class)
        ->alias('twig.command.debug_alias', 'foo_alias')
    ;
};