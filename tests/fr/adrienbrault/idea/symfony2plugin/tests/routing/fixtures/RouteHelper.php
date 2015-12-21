<?php

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
    }
}