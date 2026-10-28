<?php

namespace PHPUnit\Framework {
    abstract class TestCase {}
}

namespace Symfony\Component\DependencyInjection\Compiler {
    interface CompilerPassInterface {}
}

namespace Symfony\Component\Validator {
    abstract class Constraint {}
}

namespace App {
    use Doctrine\ORM\Mapping as ORM;
    use Symfony\Component\JsonStreamer\Attribute\JsonStreamable;
    use Symfony\Component\Messenger\Attribute\AsMessage;

    #[ORM\Entity]
    class Customer {}

    #[ORM\Entity]
    class Order {
        public function __construct(Customer $customer) {}
    }

    #[ORM\Embeddable]
    class Address {}

    #[ORM\MappedSuperclass]
    class BaseModel {}

    #[AsMessage]
    class Message {}

    #[JsonStreamable]
    class StreamableData {}

    #[\Attribute]
    class CustomAttribute {}

    abstract class BaseTest extends \PHPUnit\Framework\TestCase {}
    class ApplicationTest extends BaseTest {}

    class CompilerPass implements \Symfony\Component\DependencyInjection\Compiler\CompilerPassInterface {}
    class ValidationConstraint extends \Symfony\Component\Validator\Constraint {}

    enum Status { case Open; }

    /** @ORM\Entity */
    class AnnotationEntity {}

    class Service {}
}
