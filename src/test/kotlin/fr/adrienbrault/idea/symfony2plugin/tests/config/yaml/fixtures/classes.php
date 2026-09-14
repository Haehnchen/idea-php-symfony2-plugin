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

namespace Psr\Log
{
    interface LoggerInterface
    {
    }
}

namespace BindArgument
{
    class Consumer
    {
        function __construct($proxyUrl, string $defaultUri, iterable $rules, \Psr\Log\LoggerInterface $logger)
        {
        }
    }

    class MismatchConsumer
    {
        function __construct(int $defaultUri)
        {
        }
    }
}
