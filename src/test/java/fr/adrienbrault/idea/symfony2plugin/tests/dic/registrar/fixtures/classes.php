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
         */
        public function __construct(
            string $value = null,
            string $service = null,
            string $expression = null,
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
    }
}

