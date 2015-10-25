<?php

namespace Doctrine\ORM\Mapping {
    class Entity {};
}

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
    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @ORM\Entity(repositoryClass="FooBundle\BarRepository")
     */
    class Bar
    {
    }
}

namespace FooBundle\Entity\Car
{
    class Bar
    {
    }
    class BarRepository
    {
    }
}

namespace FooBundle
{
    class BarRepository
    {
    }
}

namespace FooBundle\Document
{
    class Doc
    {
    }
}
