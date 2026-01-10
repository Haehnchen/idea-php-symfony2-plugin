<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use App\Service\TestClassA;
use App\Service\TestClassB;
use App\Service\TestClassC;

return static function (ContainerConfigurator $container) {
    $services = $container->services();

    $services
        ->set('test.service_direct', TestClassA::class)
        ->alias('test.service_alias', 'foo_alias')
    ;

    $services->set('test.service_chained_class')
        ->class(TestClassB::class)
        ->tag('test.tag');

    // Test chained set calls - should not confuse services
    $services->set('service1')
        ->class(TestClassA::class)
        ->set('service2')
        ->class(TestClassC::class);
};
