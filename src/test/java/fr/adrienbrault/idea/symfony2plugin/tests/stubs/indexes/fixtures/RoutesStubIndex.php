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
         * @Route("/edit/{!id<.*>}/{!id<\d+>}////", name="blog_home_special")
         * @Method("GET")
         */
        public function specialAction()
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
         * @Route
         */
        public function emptyAnnotation()
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

    use Symfony\Component\HttpFoundation\Request;
    use Symfony\Component\Routing\Annotation\Route;

    class DefaultController
    {
        private const FOOBAR = 'attributes-names-foobar-const';
        private const FOOBAR_URL = '/attributes-path-foobar';

        /**
         * @Route("/", name="framework_extra_bundle_route")
         */
        public function fooAction()
        {
        }

        #[Route('/attributes-action', name: 'attributes_action')]
        public function attributesAction()
        {
        }

        #[Route('/attributesWithoutName', methods: ['GET', 'POST'])]
        public function attributesWithoutName()
        {
        }

        #[Route('/attributesWithoutNameWithConstantsInMethods', name: 'attributesWithoutNameWithConstantsInMethods', methods: [Request::METHOD_GET])]
        public function attributesWithoutNameWithConstantsInMethods()
        {
        }

        #[Route(path: '/attributes-path', name: 'attributes-names')]
        public function attributesPath()
        {
        }

        #[Route(path: self::FOOBAR_URL, name: self::FOOBAR)]
        public function attributesPathFoo1()
        {
        }

        #[Route(self::FOOBAR_URL, name: "attributes-default-as-const")]
        public function attributesPathFoo2()
        {
        }

        #[Route('/blog/{page<\d+>?1}/{page1<\d+>?}/{page2<\d+>?}/{parameter_name?default_value}', name: "blog_home_the_special_placeholder_one")]
        public function theSpecialPlaceholderOne()
        {
        }

        #[Route([
            'cs' => '/foobar/baz/{id}',
            'sk' => '/foobarbaz/bat/{id}',
        ], name: 'dashboard', methods: ['GET'])]
        public function nameAsArray()
        {
        }
    }
}


namespace MyAttributesPrefix
{
    use Symfony\Component\Routing\Annotation\Route;

    #[Route(path: '/foo-attributes', name: 'foo-attributes_')]
    class PrefixController
    {
        #[Route(path: '/edit/{id}', name: 'prefix_home')]
        public function editAction()
        {
        }
    }

    #[Route('/foo-attributes', 'foo-attributes-not-named_')]
    class PrefixNotNamedController
    {
        #[Route('/edit-not-named/{id}', 'prefix_home_not_named')]
        public function editAction()
        {
        }
    }

    #[Route('/foo-attributes-default')]
    class PrefixDefaultParameterController
    {
        #[Route(path: '/edit/{id}', name: 'prefix_home_default_parameter')]
        public function editAction()
        {
        }

        #[Route]
        public function emptyAttribute()
        {
        }
    }
}


namespace AttributeInvoke
{
    use Symfony\Component\Routing\Annotation\Route;

    #[Route(path: '/foo-attributes', name: 'invoke_route_attribute')]
    class MyController
    {
        public function __invoke()
        {
        }
    }

    #[Route(path: '/foo-attributes/no-name')]
    class NoNameController
    {
        public function __invoke()
        {
        }
    }
}

namespace Symfony\Component\Routing\Annotation
{
    class Route
    {
        public function __construct(
            $data = [],
            $path = null,
            string $name = null,
            array $requirements = [],
            array $options = [],
            array $defaults = [],
            string $host = null,
            array $methods = [],
            array $schemes = [],
            string $condition = null,
            int $priority = null,
            string $locale = null,
            string $format = null,
            bool $utf8 = null,
            bool $stateless = null
        ) {
        }
    }
}

namespace Symfony\Component\HttpFoundation
{
    class Request
    {
        public const METHOD_GET = 'GET';
    }
}

