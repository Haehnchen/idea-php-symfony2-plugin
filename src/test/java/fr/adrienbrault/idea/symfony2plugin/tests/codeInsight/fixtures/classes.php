<?php

namespace Symfony\Component\Routing\Annotation
{
    /**
     * @Annotation
     */
    class Route {}
}

namespace Symfony\Component\Console\Attribute
{
    class AsCommand
    {
    }
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

    final class Event {}
}

namespace Symfony\Component\EventDispatcher\Attribute
{
    class AsEventListener
    {
    }
}

namespace Symfony\Component\HttpKernel
{
    final class KernelEvents
    {
        public const EXCEPTION = 'kernel.exception';
    }
}

namespace Symfony\Component\Messenger\Attribute
{
    class AsMessageHandler
    {
    }
}

namespace Symfony\Component\Scheduler\Attribute
{
    class AsSchedule
    {
    }
}

namespace Symfony\Component\Workflow\Attribute
{
    class AsAnnounceListener
    {
    }

    class AsCompletedListener
    {
    }

    class AsEnterListener
    {
    }

    class AsEnteredListener
    {
    }

    class AsGuardListener
    {
    }

    class AsLeaveListener
    {
    }

    class AsTransitionListener
    {
    }
}

namespace Symfony\UX\TwigComponent\Attribute
{
    class AsTwigComponent
    {
    }
}

namespace Symfony\Component\Security\Core\Authorization\Voter
{
    interface VoterInterface
    {
        public function vote(TokenInterface $token, $subject, array $attributes);
    }
}

namespace Twig\Extension
{
    interface ExtensionInterface
    {
        public function getFilters();
        public function getFunctions();
    }
}

namespace Doctrine\ORM
{
    class EntityRepository
    {
    }
}

namespace Doctrine\ORM\Mapping
{
    class Entity {};
    class PrePersist {};
    class PreFlush {};
    class PostLoad {};
}

namespace Doctrine\ORM\Event
{
    class PrePersistEventArgs
    {
    }

    class PreFlushEventArgs
    {
    }

    class PostLoadEventArgs
    {
    }

    class PostPersistEventArgs
    {
    }
}

namespace App\Entity
{
    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @ORM\Entity(repositoryClass="App\Repository\MyFoobarEntityRepository")
     */
    class FooEntity
    {
    }
}

namespace Symfony\Component\Validator
{
    abstract class Constraint
    {
    }
    interface ConstraintValidatorInterface
    {
    }
}

namespace Symfony\Component\Validator\Constraints
{
    class Callback
    {
    }
}

namespace Symfony\Component\DependencyInjection\Attribute
{
    class AutoconfigureTag
    {
    }
}

namespace Doctrine\Common\EventSubscriber
{
    interface EventSubscriber
    {
    }
}

namespace Doctrine\ORM
{
    final class Events
    {
        public const preFlush = 'preFlush';
        public const postPersist = 'postPersist';
    }
}

namespace Doctrine\Bundle\DoctrineBundle\Attribute
{
    class AsDoctrineListener
    {
    }

    class AsEntityListener
    {
    }
}

namespace Mcp\Capability\Attribute
{
    class McpTool
    {
    }

    class McpPrompt
    {
    }

    class McpResource
    {
    }

    class McpResourceTemplate
    {
    }
}

namespace App\Validator
{
    use Symfony\Component\Validator\Constraint;

    class MyFoobarConstraintValidator extends Constraint {}
}
