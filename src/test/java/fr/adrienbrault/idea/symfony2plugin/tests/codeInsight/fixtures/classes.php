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

namespace App\Validator
{
    use Symfony\Component\Validator\Constraint;

    class MyFoobarConstraintValidator extends Constraint {}
}

