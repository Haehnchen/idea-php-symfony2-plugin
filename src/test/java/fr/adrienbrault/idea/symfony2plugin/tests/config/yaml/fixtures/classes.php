<?php

namespace
{
    class MyDateTime
    {
    }
}

namespace Foo
{
    class Bar
    {
        function __construct($i, $z) { }
        function setBar() {}
    }

    class Apple
    {
        function __construct($i, $z = null) { }
    }

    class Car
    {
        function __construct(\string $projectDir, \string $foobarEnv, \MyDateTime $myDateTime)
        {
        }
    }

    class Bus
    {
        function __construct($myBus) { }
    }
}