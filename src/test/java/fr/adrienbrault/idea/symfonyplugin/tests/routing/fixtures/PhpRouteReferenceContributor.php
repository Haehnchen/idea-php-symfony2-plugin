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

namespace Symfony\Bundle\FrameworkBundle\Controller
{
    class Controller
    {
        public function generateUrl($name) {}
        public function redirectToRoute($name) {}
    }
}

namespace My\Proxy\Routing
{
    class Controller
    {
        public function generateUrl($name)
        {
            /** @var \Symfony\Bundle\FrameworkBundle\Controller\Controller $foo */
            $foo->generateUrl($name);
        }
    }
}