<?php

namespace Symfony\Component\Routing\Generator
{
    abstract class UrlGenerator implements UrlGeneratorInterface {};
    interface UrlGeneratorInterface {}
}
namespace Symfony\Component\Routing\Loader\Configurator
{
    use Symfony\Component\Routing\Loader\Configurator\Traits\AddTrait;
    use Symfony\Component\Routing\Loader\Configurator\Traits\RouteTrait;

    class RouteConfigurator
    {
        use AddTrait;
        use RouteTrait;
    }

    class RoutingConfigurator
    {
        use AddTrait;
    }
}

namespace Symfony\Component\Routing\Loader\Configurator\Traits
{
    use Symfony\Component\Routing\Loader\Configurator\RouteConfigurator;

    trait RouteTrait
    {
        final public function controller(callable|string|array $controller): static
        {
        }
    }

    trait AddTrait
    {
        public function add(string $name, string|array $path): RouteConfigurator
        {
        }
    }
}