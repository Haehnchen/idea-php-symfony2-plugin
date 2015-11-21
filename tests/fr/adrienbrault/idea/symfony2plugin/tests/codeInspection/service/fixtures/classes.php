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