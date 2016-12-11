<?php

namespace
{
    class MyDateTime
    {
        public function setTimestamp() {}
    }
}

namespace Symfony\Component\Form {
    interface FormTypeInterface {
        public function setDefaultOptions(OptionsResolverInterface $resolver);
        public function configureOptions(OptionsResolver $resolver);
        public function getName();
    }
    interface FormBuilderInterface {
        public function add($child, $type = null, array $options = array());
        public function create($name, $type = null, array $options = array());
        public function get($name);
        public function remove($name);
        public function has($name);
        public function all();
        public function getForm();
    }

    interface FormInterface {
        public function add();
        public function create();
    }

    interface FormTypeExtensionInterface {}
}


namespace Symfony\Component\Form\Extension\Core\Type {
    use Symfony\Component\Form\FormTypeExtensionInterface;
    use Symfony\Component\Form\FormTypeInterface;

    class FormType implements FormTypeInterface, FormTypeExtensionInterface
    {
        public function setDefaultOptions(OptionsResolverInterface $resolver)
        {
            $resolver->setDefaults(array(
                'default_options' => null,
            ));

            $resolver->setDefined(array('default_setDefined'));
            $resolver->setOptional(array('default_setOptional'));
            $resolver->setRequired(array('default_setRequired', 'default_setRequired2'));
        }

        public function configureOptions(OptionsResolverInterface $resolver)
        {
            $resolver->setDefaults(array(
                'configure_options' => null,
            ));

            $resolver->setDefined(array('configure_setDefined'));
            $resolver->setOptional(array('configure_setOptional'));
            $resolver->setRequired(array('configure_setRequired', 'configure_setRequired2'));
        }

        public function getName()
        {
            return 'form';
        }

        public function getExtendedType() {
            return 'form';
        }

    }
}

namespace Form\FormType {

    use Symfony\Component\Form\FormTypeInterface;

    class Foo implements FormTypeInterface
    {
        public function getName()
        {
            return 'foo_type';
        }
    }
}

namespace Form\DataClass
{
    class Model {
        public $var;
        public $varBar;
        
        public function setFooBar()
        {
        }
    }
}

namespace Symfony\Component\OptionsResolver
{
    class OptionsResolver
    {
        public function setDefault($option, $value) {}
        public function hasDefault($option) {}
        public function isRequired($option) {}
        public function isMissing($option) {}
        public function setAllowedValues($option) {}
        public function addAllowedValues($option) {}
        public function setAllowedTypes($option) {}
        public function addAllowedTypes($option) {}
    }
}