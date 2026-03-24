<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use App\Service\MyService;
use App\Util\MessageGenerator;

return App::config([
    'services' => [
        '_defaults' => [
            'autowire' => true,
            'public' => false,
        ],
        MessageGenerator::class => null,
        'app.my_service' => [
            'class' => MyService::class,
            'tags' => [
                'my.tag',
                ['name' => 'my.named_tag'],
            ],
        ],
        'app.alias_service' => '@app.my_service',
        'app.decorated_service' => [
            'class' => \ArrayObject::class,
            'decorates' => 'app.my_service',
            'decoration_inner_name' => 'app.decorated_service.inner_custom',
        ],
        'app.resource_prototype' => [
            'resource' => ['../src/*', '../src2/*'],
            'exclude' => '../src/{Tests,Kernel.php}',
            'autowire' => false,
        ],
    ],
]);
