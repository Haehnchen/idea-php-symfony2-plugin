<?php

namespace Symfony\Component\DependencyInjection
{
    class Definition
    {
        public function addTag($name, array $attributes = array())
        {
        }

        public function hasTag($name)
        {
        }

        public function clearTag($name)
        {
        }
    }

    class ContainerBuilder
    {
        public function hasDefinition(string $id)
        {
        }

        public function getDefinition(string $id)
        {
        }

        public function setAlias(string $id, string $referencedId)
        {
        }

        public function findDefinition(string $id)
        {
        }

        public function removeDefinition(string $id)
        {
        }

        public function removeAlias(string $id)
        {
        }
    }
}

namespace Symfony\Component\DependencyInjection\Loader\Configurator
{
    class ContainerConfigurator
    {
        public function services(): ServicesConfigurator
        {
            return new ServicesConfigurator();
        }
    }

    class ServicesConfigurator
    {
        public function set(?string $id, ?string $class = null): ServiceConfigurator
        {
        }

        public function alias(string $id, string $referencedId): ServiceConfigurator
        {
        }
    }

    abstract class AbstractServiceConfigurator
    {
        final public function alias(string $id, string $referencedId): ServiceConfigurator
        {
        }
    }

    class ServiceConfigurator extends AbstractServiceConfigurator
    {
    }
}

namespace Symfony\Component\Messenger\Handler
{
    interface MessageSubscriberInterface
    {
        public static function getHandledMessages(): iterable;
    }
}

namespace Symfony\Component\EventDispatcher
{
    interface EventSubscriberInterface
    {
    }
}

namespace Symfony\Component\EventDispatcher\Attribute
{
    #[\Attribute(\Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD | \Attribute::IS_REPEATABLE)]
    class AsEventListener
    {
        public function __construct(
            public ?string $event = null,
            public ?string $method = null,
            public int $priority = 0,
            public ?string $dispatcher = null,
        ) {
        }
    }
}
