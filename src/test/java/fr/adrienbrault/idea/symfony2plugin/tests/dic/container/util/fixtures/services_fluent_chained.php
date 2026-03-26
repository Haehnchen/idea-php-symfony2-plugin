<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use App\Service\TestClassA;
use App\Service\TestClassB;
use App\Service\TestClassC;

return static function (ContainerConfigurator $container) {
    // Parameters must NOT be indexed as services
    $container->parameters()
        ->set('my.parameter', 'some_value')
    ;

    $parameters = $container->parameters();
    $parameters->set('my.parameter_variable', 'some_value');

    // All services chained directly on ->services() without a variable
    $container->services()

        ->set('chain.service_a', TestClassA::class)
            ->args([
                service('chain.service_b'),
            ])
            ->tag('app.handler')

        ->set('chain.service_b', TestClassB::class)
            ->public()
            ->tag('app.processor')
            ->tag('app.secondary')

        ->set('chain.decorated', TestClassC::class)
            ->decorate('chain.service_a', 'chain.decorated.inner')

        ->set('chain.lazy_autowired', TestClassA::class)
            ->lazy()
            ->autowire()

        ->set('chain.last', TestClassB::class)
    ;
};
