<?php

namespace Sensio\Bundle\FrameworkExtraBundle\Configuration
{
    /**
     * @Annotation
     */
    class Route {}
}

namespace MyFooBarBundle\Controller
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;

    /**
     * @Route("/foo_bar")
     */
    class CarController
    {
        /**
         * @Route("/edit/{id}")
         */
        public function indexAction()
        {
        }

        /**
         * @Route("/edit/{id}", name="my_car_foo_stuff")
         */
        public function fooAction()
        {
        }

        /**
         * @Route("/edit/{id}", name="my_car_foo_stuff_2")
         */
        public function foo2Action()
        {
        }

        /**
         * @Route("/foobar/edit/{id}/foo/{foo}")
         */
        public function indexFooBarEditAction()
        {
        }
    }

    /**
     * @Route("/foo_bar", name="foobar_")
     */
    class AppleController
    {
        /**
         * @Route("/edit/{id}")
         */
        public function indexAction()
        {
        }

        /**
         * @Route("/edit/{id}", name="my_foo")
         */
        public function fooAction()
        {
        }

        /**
         * @Route("/resolve/me/{id<\d>}/foobar/item", name="my_foo_resolve_me")
         */
        public function resolveMe()
        {
        }
    }

    class InvokeController
    {
        /**
         * @Route("/edit/{id}")
         */
        public function __invoke()
        {
        }
    }

    class CarAttributeController
    {
         #[Route(path: '/edit/{id}', name: 'my_car_foo_stuff_attribute')]
        public function fooAction()
        {
        }
    }
}