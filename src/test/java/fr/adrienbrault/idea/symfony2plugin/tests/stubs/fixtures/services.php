<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use My\Foo\Service\PhpTargets;

return static function (ContainerConfigurator $container) {
    $container->services()
        ->set('php_twig.command.debug', PhpTargets::class)
        ->alias('php_twig.command.debug_alias', 'php_foo_alias')
    ;
};
