<?php

namespace Symfony\Component\Routing\Generator
{
    interface UrlGeneratorInterface
    {
        public function generate(string $name, array $parameters = [], int $referenceType = self::ABSOLUTE_PATH);
    }
}

namespace App\Service
{
    interface FoobarInterface
    {
        public function bar();
    }

    interface InterfaceFoobarCar
    {
        public function bar();
    }

    class FoobarLongClassNameServiceFactory
    {
    }
}

namespace Psr\Log
{
    interface LoggerInterface() {}
}