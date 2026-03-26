<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use App\Service\TestClassA;
use App\Service\TestClassB;
use App\Service\TestClassC;

return static function (ContainerConfigurator $container) {
    $services = $container->services();

    $services->defaults()
        ->autowire()
        ->public(false);

    // Inherits defaults: autowire=true, public=false
    $services->set('defaults.inherited', TestClassA::class);

    // Overrides autowire to false, keeps public=false from defaults
    $services->set('defaults.autowire_override', TestClassB::class)
        ->autowire(false);

    // Overrides public to true, keeps autowire=true from defaults
    $services->set('defaults.public_override', TestClassC::class)
        ->public();
};
