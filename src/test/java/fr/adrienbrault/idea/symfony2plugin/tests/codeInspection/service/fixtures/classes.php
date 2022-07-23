<?php

namespace Foo\Bar
{
    /**
     * @deprecated
     */
    class FooBar {}
}

namespace Symfony\Component\DependencyInjection
{
    interface ContainerInterface
    {
        function get();
    }
}

namespace
{
    class Twig_Extension {}
    interface Twig_ExtensionInterface {}
}

namespace Tag\InstanceCheck
{
    class EmptyClass {}
}

namespace Symfony\Component\DependencyInjection\Attribute
{
    class Autowire {}
}