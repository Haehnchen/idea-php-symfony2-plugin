<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

return static function (ContainerConfigurator $container) {
    return [
        'services' => [
            '_defaults' => [
                'autowire' => true,
            ],
            'App\\ClosureService\\' => [
                'resource' => '../ClosureService/*',
            ],
        ],
    ];
};
