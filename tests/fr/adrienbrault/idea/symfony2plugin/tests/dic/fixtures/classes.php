<?php

namespace
{
    class MyDateTime
    {
        public function format() {}
    }
}

namespace Symfony\Component\DependencyInjection {
    interface ContainerInterface {
        function get($name);
    };
}

namespace Foo\Bar
{
    class Bar
    {
        public function getBar()
        {
        }
    }    
}