<?php

namespace Symfony\Component\Form
{
    interface FormFactoryInterface
    {
        public function create(string $type = FormType::class, mixed $data = null, array $options = []) {}
        public function createBuilder(string $type = FormType::class, mixed $data = null, array $options = []): FormBuilderInterface;
        public function createNamed(string $name, string $type = FormType::class, mixed $data = null, array $options = []): FormInterface;
        public function createNamedBuilder(string $name, string $type = FormType::class, mixed $data = null, array $options = []): FormBuilderInterface;
    }
}

namespace App
{
    class MyFormType {}
}