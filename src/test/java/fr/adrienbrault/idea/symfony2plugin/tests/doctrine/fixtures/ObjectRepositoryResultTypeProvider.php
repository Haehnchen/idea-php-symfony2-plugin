<?php

namespace Foo
{
    use Doctrine\Common\Persistence\ObjectRepository;

    class Bar
    {
        public function getId()
        {
        }
    }

    class BarRepository implements ObjectRepository
    {
        public function find() {}

        /**
         * @return Bar[]
         */
        public function findOneByFancyStuff() {}
    }
}


namespace Doctrine\Common\Persistence
{

    interface ObjectManager
    {
        /**
         * @param $class
         * @return ObjectRepository
         */
        function getRepository($class);
    }

    interface ObjectRepository
    {
        /**
         * @return \DateTime
         */
        function find();
        function findOne();
        function findOneBy();
        function findAll();
        function findBy();
    }
}