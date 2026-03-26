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

    // Test full chain attribute extraction
    $services->set('test.full_chain', TestClassA::class)
        ->tag('chain.tag_one')
        ->tag('chain.tag_two')
        ->public()
        ->autowire()
        ->lazy()
        ->deprecated();

    // Test autowire(false) / public(false) explicit override
    $services->set('test.autowire_false', TestClassA::class)
        ->autowire(false)
        ->public(false);

    $services->set('test.decorated', TestClassB::class)
        ->decorate('test.service_direct');

    $services->set('test.decorated_with_inner', TestClassC::class)
        ->decorate('test.service_direct', 'test.decorated_with_inner.custom');

    $services->set('test.with_parent', TestClassA::class)
        ->parent('test.service_direct');

    $services->set('test.private', TestClassA::class)
        ->private();
};
