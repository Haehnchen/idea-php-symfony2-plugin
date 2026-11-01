<?php

use Symfony\Component\DependencyInjection\Loader\Configurator\ContainerConfigurator;

return static function (ContainerConfigurator $container): void {
    $container->extension('ux_css', [
        'semantic_tokens' => [
            'colors' => [
                'muted' => '{colors.gray.900}',
            ],
        ],
    ]);
};
