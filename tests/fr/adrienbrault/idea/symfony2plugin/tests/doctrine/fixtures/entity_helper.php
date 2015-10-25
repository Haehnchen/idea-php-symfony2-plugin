<?php

namespace Symfony\Component\HttpKernel\Bundle
{
    interface Bundle {}
}

namespace FooBundle
{
    use Symfony\Component\HttpKernel\Bundle\Bundle;
    class FooBundle implements Bundle {}
}

namespace FooBundle\Entity
{
    class Bar
    {
    }
}

namespace FooBundle\Entity\Car
{
    class Bar
    {
    }
}

namespace FooBundle\Document
{
    class Doc
    {
    }
}
