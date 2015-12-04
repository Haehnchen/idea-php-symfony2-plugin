<?php

namespace Form\FormType
{
    class Foo
    {
        public function getName()
        {
            return 'foo_type';
        }
    }

    class FooBar
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