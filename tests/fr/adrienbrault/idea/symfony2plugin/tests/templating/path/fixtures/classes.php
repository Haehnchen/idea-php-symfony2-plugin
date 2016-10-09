<?php

namespace Symfony\Component\HttpKernel\Bundle
{
    interface Bundle
    {
    }
}

namespace FooBundle
{
    use Symfony\Component\HttpKernel\Bundle\Bundle;

    class FooBundle implements Bundle
    {
    }
}