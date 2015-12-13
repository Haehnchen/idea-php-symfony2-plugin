<?php

namespace Sensio\Bundle\FrameworkExtraBundle\Configuration
{
    interface Route
    {
    }
}

namespace Foo\Route
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;

    class MyController
    {

        /**
         * @Route("/foo/{bar}", name = "foo_bar")
         */
        public function myAction($bar)
        {
        }
    }
}

namespace Symfony\Component\Routing\Generator
{
    interface UrlGeneratorInterface {
        public function generate();
    };
}