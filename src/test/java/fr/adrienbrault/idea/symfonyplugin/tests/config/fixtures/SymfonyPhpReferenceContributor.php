<?php

namespace Doctrine\Common\Persistence {
    interface ObjectRepository
    {
        public function findOneBy();
        public function findBy();
    }

    interface ObjectManager
    {
        /**
         * @return \Doctrine\Common\Persistence\ObjectRepository
         */
        public function getRepository();
    }
}


namespace Doctrine\ORM\Mapping {
    class Entity {};
    class Column {};
    class OneToMany {};
}

namespace Foo {

    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @ORM\Entity()
     */
    class Bar {

        /**
         * @ORM\Column(type="string")
         */
        private $email;

        /**
         * @ORM\OneToMany(targetEntity="Phonenumber")
         */
        private $phonenumbers;

    };

    class Car {}
}

namespace Symfony\Component\DependencyInjection
{
    interface ContainerInterface
    {
        public function get();
    }
}