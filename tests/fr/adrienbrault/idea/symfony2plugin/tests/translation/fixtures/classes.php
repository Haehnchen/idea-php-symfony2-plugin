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

namespace Symfony\Component\Validator\Context
{
    interface ExecutionContextInterface
    {
        public function addViolation($message, array $params = array());
        public function buildViolation($message, array $parameters = array());
    }
}

namespace
{
    use Symfony\Component\Validator\Constraint;

    class MyConstraintMessage extends Constraint
    {
    };
}

namespace Symfony\Component\Validator\Violation
{
    interface ConstraintViolationBuilderInterface
    {
        public function setTranslationDomain($translationDomain);
    }
}