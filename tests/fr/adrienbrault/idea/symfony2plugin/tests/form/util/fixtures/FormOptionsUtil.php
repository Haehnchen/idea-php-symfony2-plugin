<?php

namespace Foo\Bar
{
    use Symfony\Component\Form\FormTypeExtensionInterface;

    class MyType implements FormTypeExtensionInterface {
        public function getExtendedType()
        {
            return 'foo';
        }
    }

    class BarType implements FormTypeExtensionInterface {
        public function getExtendedType()
        {
            return MyType::class;
        }
    }
}