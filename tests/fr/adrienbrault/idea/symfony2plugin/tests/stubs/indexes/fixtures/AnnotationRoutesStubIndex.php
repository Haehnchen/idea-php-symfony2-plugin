<?php

namespace My
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;

    /**
     * @Route("/foo")
     */
    class PostController
    {
        /**
         * @Route("/edit/{id}", name="blog_home")
         */
        public function editAction()
        {
        }
    }
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
    }
}