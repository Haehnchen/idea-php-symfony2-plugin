<?php

declare(strict_types=1);

namespace {{ namespace }};

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class {{ class }} extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options) : void
    {
    }

    public function configureOptions(OptionsResolver $resolver) : void
    {
    }
}
