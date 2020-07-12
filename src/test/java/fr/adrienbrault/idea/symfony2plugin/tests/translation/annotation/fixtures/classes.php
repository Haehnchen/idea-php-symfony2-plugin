<?php

namespace Symfony\Component\Validator
{
    abstract class Constraint
    {
        public function __construct($options = null)
        {
        }
    }
}

namespace Symfony\Component\Validator\Constraints
{
    use Symfony\Component\Validator\Constraint;

    class Email extends Constraint
    {
        public $message = 'This value is not a valid email address.';
    }
}
