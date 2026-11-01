<?php

use Symfony\Component\DependencyInjection\Loader\Configurator\App;

return App::config([
    'ux_css' => [
        'tokens' => [
            'colors' => [
                'accent' => 'oklch(70.5% 0.213 47.604)',
                'gray' => [
                    900 => '#111827',
                ],
            ],
        ],
    ],
]);
