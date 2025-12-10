<?php

namespace Symfony\Component\DependencyInjection
{
    interface ContainerInterface
    {
        function getParameter($parameter);
        function hasParameter($parameter);
    }
}

namespace Symfony\Component\DependencyInjection\Attribute
{
    class Autowire
    {
        /**
         * @param string|null $value      Parameter value (ie "%kernel.project_dir%/some/path")
         * @param string|null $service    Service ID (ie "some.service")
         * @param string|null $expression Expression (ie 'service("some.service").someMethod()')
         * @param string|null $param      Container parameter name (ie "kernel.debug")
         * @param string|null $env        Environment variable name (ie "DATABASE_URL")
         */
        public function __construct(
            string $value = null,
            string $service = null,
            string $expression = null,
            string $param = null,
            string $env = null,
        ) {}
    }

    class TaggedIterator
    {
        public function __construct(
            public string $tag,
            public ?string $indexAttribute = null,
            public ?string $defaultIndexMethod = null,
            public ?string $defaultPriorityMethod = null,
            public string|array $exclude = [],
        ) {}
    }

    class TaggedLocator
    {
        public function __construct(
            public string $tag,
            public ?string $indexAttribute = null,
            public ?string $defaultIndexMethod = null,
            public ?string $defaultPriorityMethod = null,
            public string|array $exclude = [],
        ) {
        }
    }

    class AsDecorator
    {
        public function __construct(
            public string $decorates,
            public int $priority = 0,
            public int $onInvalid = ContainerInterface::EXCEPTION_ON_INVALID_REFERENCE,
        ) {
        }
    }

    class Autoconfigure
    {
        public function __construct(
            public ?array $tags = null,
            public ?array $calls = null,
            public ?array $bind = null,
            public bool|string|null $lazy = null,
            public ?bool $public = null,
            public ?bool $shared = null,
            public ?bool $autowire = null,
            public ?array $properties = null,
            public array|string|null $configurator = null,
        ) {
        }
    }

    class AutowireServiceClosure
    {
        public function __construct(
            public ?string $service = null,
        ) {
        }
    }

    class AutowireCallable
    {
        public function __construct(
            public ?string $service = null,
            public ?string $method = null,
        ) {
        }
    }

    class AutowireMethodOf
    {
        public function __construct(
            public ?string $service = null,
        ) {
        }
    }
}

namespace
{
    class Foo
    {

        /**
         * @var \Symfony\Component\DependencyInjection\ContainerInterface
         */
        private $container;

        public function foo($parameter) {
            return $this->container->getParameter($parameter);
        }

        public function bar($parameter) {
            return $this->container->hasParameter($parameter);
        }
    }
}

namespace Foo
{
    class Bar
    {
        public function format($message) {
            return strtoupper($message);
        }

        public function process($data) {
            return $data;
        }

        private function privateMethod() {
            return 'private';
        }

        public static function staticMethod() {
            return 'static';
        }
    }
}

