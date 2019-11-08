<?php

namespace Symfony\Component\HttpKernel\Bundle
{
    interface Bundle {}
}

namespace FooBundle
{
    use Symfony\Component\HttpKernel\Bundle\Bundle;
    class FooBundle implements Bundle {}
}

namespace FooBundle\Controller
{
    class FoobarController
    {
        public function barAction()
        {
        }

        public function __invoke()
        {
        }
    }
}