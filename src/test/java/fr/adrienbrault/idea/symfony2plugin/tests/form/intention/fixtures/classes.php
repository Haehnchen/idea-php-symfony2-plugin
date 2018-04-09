<?php

namespace Symfony\Component\Form
{
    interface FormBuilderInterface
    {
        public function add();
        public function create();
    }

    interface FormFactoryInterface
    {
        public function createNamedBuilder();
        public function createNamed();
    }
}

namespace Symfony\Component\Form
{
    interface FormTypeInterface
    {
    }
}

namespace Symfony\Component\Form\Extension\Core\Type
{
    use Symfony\Component\Form\FormTypeInterface;

    class HiddenType implements FormTypeInterface
    {
    }
}


