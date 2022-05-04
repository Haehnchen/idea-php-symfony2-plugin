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

namespace Symfony\Component\Security\Core\Authorization\Voter
{
    interface VoterInterface
    {
        public function vote(TokenInterface $token, $subject, array $attributes);
    }
}