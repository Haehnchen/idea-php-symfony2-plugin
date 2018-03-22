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
}

namespace Tag\InstanceCheck
{
    class EmptyClass {}
}