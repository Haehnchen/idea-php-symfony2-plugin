<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use My\Foo\Service\PhpArrayDefaults;
use My\Foo\Service\PhpArrayTargets;

return App::config([
    'services' => [
        '_defaults' => [
            'autowire' => true,
            'public' => false,
        ],
        PhpArrayDefaults::class => null,
        'php_array.service' => [
            'class' => PhpArrayTargets::class,
            'tags' => [
                'php_array_tag',
                ['name' => 'php_array_named_tag'],
            ],
        ],
        'php_array.alias' => '@php_array.service',
        'php_array.decorated' => [
            'class' => \ArrayObject::class,
            'decorates' => 'php_array.service',
            'decoration_inner_name' => 'php_array.decorated.inner_custom',
        ],
        'php_array.resource' => [
            'resource' => ['../src/*', '../src2/*'],
            'exclude' => '../src/{Tests,Kernel.php}',
            'autowire' => false,
        ],
    ],
]);
