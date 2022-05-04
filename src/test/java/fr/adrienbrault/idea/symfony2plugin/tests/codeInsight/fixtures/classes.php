<?php

namespace Symfony\Component\Routing\Annotation
{
    /**
     * @Annotation
     */
    class Route {}
}

namespace Symfony\Component\Console\Command
{
    class Command
    {
    }
}

namespace Symfony\Component\EventDispatcher
{
    interface EventSubscriberInterface
    {
        public static function getSubscribedEvents();
    }
}