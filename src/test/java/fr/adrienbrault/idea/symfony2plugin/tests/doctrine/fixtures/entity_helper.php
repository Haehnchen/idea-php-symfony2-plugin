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

    use Doctrine\Common\Persistence\ObjectRepository;
    use Doctrine\ORM\Mapping AS ORM;
    use FooBundle\Entity AS FooAlias;

    /**
     * @ORM\Entity(repositoryClass="BarRepository")
     */
    class Bar
    {
    }

    /**
     * @ORM\Entity(repositoryClass=BarRepository::class)
     */
    class Bar2
    {
    }

    /**
     * @ORM\Entity(repositoryClass=FooAlias\BarRepository::class)
     */
    class Bar3
    {
    }

    class Yaml
    {
    }
    
    class BarRepository implements ObjectRepository
    {
    }

    interface BarInterface {}
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

namespace FooBundle\CouchDocument
{
    class Couch
    {
    }
}

namespace Doctrine\Common\Persistence
{
    interface ObjectRepository{}
}
