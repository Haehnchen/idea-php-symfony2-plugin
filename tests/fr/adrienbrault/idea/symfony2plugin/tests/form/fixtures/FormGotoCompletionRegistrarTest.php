<?php

namespace Symfony\Component\Form
{
    use Symfony\Component\OptionsResolver\OptionsResolver;

    interface FormTypeInterface
    {
        public function configureOptions(OptionsResolver $resolver);
    }
}

namespace Symfony\Component\OptionsResolver
{
    interface OptionsResolver
    {
        public function setDefined($optionNames);
    }
}