<?php

declare(strict_types=1);

namespace {{ namespace }};

use Symfony\Component\DependencyInjection\ContainerBuilder;
use Symfony\Component\DependencyInjection\Compiler\CompilerPassInterface;

class {{ class }} implements CompilerPassInterface
{
    public function process(ContainerBuilder $container): void
    {
        // in this method you can manipulate the service container:
        // for example, changing some container service:
        $container->getDefinition('app.some_private_service')->setPublic(true);

        // or processing tagged services:
        foreach ($container->findTaggedServiceIds('some_tag') as $id => $tags) {
            // ...
        }
    }
}
