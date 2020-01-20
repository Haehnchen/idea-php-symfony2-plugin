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
