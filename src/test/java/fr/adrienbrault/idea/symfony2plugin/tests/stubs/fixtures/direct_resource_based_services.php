<?php

return [
    'services' => [
        '_defaults' => [
            'autowire' => true,
        ],
        'App\\DirectService\\' => [
            'resource' => '../DirectService/*',
        ],
    ],
];
