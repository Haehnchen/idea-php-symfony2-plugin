<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use Symfony\Bridge\Twig\Command\DebugCommand;
use Symfony\Bundle\TwigBundle\Command\LintCommand;
use Symfony\Component\DependencyInjection\Container;

return static function (ContainerConfigurator $container) {
    // Container parameters — must NOT be indexed as services
    $container->parameters()
        ->set('fluent.parameter', 'some_value')
        ->set('fluent.parameter2', 42)
    ;

    $parameters = $container->parameters();
    $parameters->set('fluent.parameter_variable', 'some_value');

    // All services chained directly on ->services() without an intermediate variable
    $container->services()

        ->set('fluent.chain.a', DebugCommand::class)
            ->tag('console.command')
            ->public()

        ->set('fluent.chain.b', LintCommand::class)
            ->tag('app.tag_one')
            ->tag('app.tag_two')
            ->lazy()
            ->autowire()

        ->set('fluent.chain.decorated', Container::class)
            ->decorate('fluent.chain.a', 'fluent.chain.decorated.inner_custom')

        ->set('fluent.chain.last', DebugCommand::class)
    ;
};
