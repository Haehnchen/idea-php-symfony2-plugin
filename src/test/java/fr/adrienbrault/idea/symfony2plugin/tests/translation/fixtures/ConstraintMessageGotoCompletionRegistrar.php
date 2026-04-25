<?php

namespace Symfony\Component\Validator
{
    abstract class Constraint
    {
    }
}

namespace Symfony\Component\Validator\Constraints
{
    use Symfony\Component\Validator\Constraint;

    /**
     * @Annotation
     */
    class NotBlank extends Constraint
    {
        public $message;

        public function __construct($message = null)
        {
        }
    }

    /**
     * @Annotation
     */
    class Length extends Constraint
    {
        public $minMessage;
        public $maxMessage;

        public function __construct($minMessage = null, $maxMessage = null)
        {
        }
    }
}
