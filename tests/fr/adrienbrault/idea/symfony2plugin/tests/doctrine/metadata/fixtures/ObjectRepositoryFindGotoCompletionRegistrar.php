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

namespace Foo
{

    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @ORM\Entity(repositoryClass="Foo\Repository\BarRepository")
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

namespace Foo\Repository
{
    use Doctrine\Common\Persistence\ObjectRepository;

    class BarRepository implements ObjectRepository {
        function findOne() {}
        function findOneBy() {}
        function findAll() {}
        function findBy() {}
        public function find() {}
    }
}