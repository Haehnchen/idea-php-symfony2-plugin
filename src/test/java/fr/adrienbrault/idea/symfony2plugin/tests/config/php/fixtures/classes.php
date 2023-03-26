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