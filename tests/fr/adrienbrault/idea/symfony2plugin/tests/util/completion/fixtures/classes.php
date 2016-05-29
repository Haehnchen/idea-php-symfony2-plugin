<?php

namespace Bar
{
    class Foo
    {
        public function process(\Symfony\Component\DependencyInjection\ContainerBuilder $container)
        {
            $container->findTaggedServiceIds('my.acme_mailer.transport.tag');
        }
    }
}
