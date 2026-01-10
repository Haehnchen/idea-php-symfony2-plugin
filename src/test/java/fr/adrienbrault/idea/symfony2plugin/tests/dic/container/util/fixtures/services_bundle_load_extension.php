<?php

namespace App\Bundle;

use App\Service\TestClassA;
use App\Service\TestClassB;
use Symfony\Component\DependencyInjection\ContainerBuilder;
use Symfony\Component\DependencyInjection\Loader\Configurator\ContainerConfigurator;
use Symfony\Component\HttpKernel\Bundle\AbstractBundle;

final class TestBundle extends AbstractBundle
{
    /**
     * @param array<string, mixed> $config
     */
    public function loadExtension(array $config, ContainerConfigurator $container, ContainerBuilder $builder): void
    {
        $services = $container->services();

        $services->set('test.bundle_service')
            ->class(TestClassA::class)
            ->tag('test.tag');

        $services->set('test.bundle_chained')
            ->class(TestClassB::class);
    }
}
