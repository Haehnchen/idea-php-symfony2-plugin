<?php

namespace My
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Method;

    /**
     * @Route("/foo")
     */
    class PostController
    {
        /**
         * @Route("/edit/{id}", name="blog_home")
         * @Method("GET")
         */
        public function editAction()
        {
        }

        /**
         * @Route("/edit/{id}", name="blog_home_get_head")
         * @Method({"GET","HEAD"})
         */
        public function getHeadAction()
        {
        }

        /**
         * @Route("/")
         */
        public function __invoke()
        {
        }
    }
}

namespace MyPrefix
{
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Route;
    use Sensio\Bundle\FrameworkExtraBundle\Configuration\Method;

    /**
     * @Route("/foo", name="foo_")
     */
    class PrefixController
    {
        /**
         * @Route("/edit/{id}", name="prefix_home")
         * @Method("GET")
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

namespace AppBundle\My\Controller
{
    use Symfony\Component\Routing\Annotation\Route;

    class DefaultController
    {
        /**
         * @Route("/", name="framework_extra_bundle_route")
         */
        public function fooAction()
        {
        }
    }
}