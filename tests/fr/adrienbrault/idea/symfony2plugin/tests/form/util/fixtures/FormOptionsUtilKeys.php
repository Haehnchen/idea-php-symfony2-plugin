<?php

namespace Options\Bar
{
    use Symfony\Component\Form\FormTypeInterface;
    use Symfony\Component\Form\OptionsResolverInterface;

    use Symfony\Component\Form\FormTypeExtensionInterface;

    class MyType implements FormTypeExtensionInterface {
        public function configureOptions(OptionsResolver $resolver)
        {
            $resolver->setDefaults(array(
                'MyType' => null,
            ));
        }

        public function getExtendedType()
        {
            return 'foo';
        }
    }

    class BarType implements FormTypeExtensionInterface {
        public function configureOptions(OptionsResolver $resolver)
        {
            $resolver->setDefaults(array(
                'BarType' => null,
            ));
        }

        public function getExtendedType()
        {
            return \Options\Bar\Foobar::class;
        }
    }

    class BarTypeExtension implements FormTypeExtensionInterface {
        public function configureOptions(OptionsResolver $resolver)
        {
            $resolver->setDefaults(array(
                'BarTypeExtension' => null,
            ));
        }

        public function getExtendedType()
        {
            return 'foo_parent';
        }
    }

    class Foo implements FormTypeInterface
    {
        public function getName()
        {
            return 'foo';
        }
        public function getParent()
        {
            return 'foo_parent';
        }
    }

    class FooParent implements FormTypeInterface
    {
        public function configureOptions(OptionsResolver $resolver)
        {
            $resolver->setDefaults(array(
                'BarTypeParent' => null,
            ));
        }

        public function getName()
        {
            return 'foo_parent';
        }
    }

    class Foobar implements FormTypeInterface
    {
    }
}