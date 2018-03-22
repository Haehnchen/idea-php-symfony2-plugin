<?php

namespace Form\FormType
{
    use Symfony\Component\Form\FormTypeInterface;

    class Foo implements FormTypeInterface
    {
        public function getName()
        {
            return 'foo_type';
        }
    }

    class FooBar implements FormTypeInterface
    {
        const FOO_BAR = 'foo_bar';

        public function getName()
        {
            return static::FOO_BAR;
        }
    }
}

namespace Symfony\Component\Form
{
    interface FormTypeInterface {}
}