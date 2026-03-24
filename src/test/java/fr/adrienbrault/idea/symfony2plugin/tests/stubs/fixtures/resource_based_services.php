<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

return App::config([
    'services' => [
        '_defaults' => [
            'autowire' => true,
        ],
        'App\\Service\\' => [
            'resource' => '../Service/*',
        ],
    ],
]);
