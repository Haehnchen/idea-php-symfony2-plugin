<?php


namespace Symfony\Component\HttpKernel\Bundle
{
    class Bundle{}
}

namespace FooBar\FooBundle
{
    use Symfony\Component\HttpKernel\Bundle\Bundle;

    class FooBarFooBundle extends Bundle{}
}

namespace Service\Controller
{
    class FooController
    {
        public function indexAction() {}   
    }    
}