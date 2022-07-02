<?php

namespace App {

    use Symfony\Component\OptionsResolver\OptionsResolver;

    class FooDataClass1 {}
    class FooDataClass2 {}
    class FooDataClass3 {}

    class AutoFarmType
    {
        public function configureOptions(OptionsResolver $resolver)
        {
            $resolver->setDefaults([
                'data_class' => FooDataClass1::class,
            ]);

            $resolver->setDefault('data_class', FooDataClass2::class);

            $resolver->setDefault('data_class', 'App\FooDataClass3');
        }
    }
}