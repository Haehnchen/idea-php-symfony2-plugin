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

namespace Foo\ParkResortBundle\Controller\SubController\BundleController
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;

    class FooController
    {
        /**
         * @Route("/")
         */
        public function nestedFooAction()
        {
        }
    }
}

namespace Foo\ParkResortBundle\Controller\SubController
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;

    class CarController
    {
        /**
         * @Route("/")
         */
        public function indexAction()
        {
        }
    }
}

namespace Foo\ParkResortBundle\Controller
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;

    class DefaultController
    {
        /**
         * @Route("/")
         */
        public function indexAction()
        {
        }
    }
}

namespace Foo\ParkResortBundle\Actions
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;

    class Foo
    {
        /**
         * @Route("/")
         */
        public function indexAction()
        {
        }
    }
}


namespace AppBundle\Controller
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;

    class DefaultController
    {
        /**
         * @Route("/")
         */
        public function fooAction()
        {
        }
    }
}