<?php

namespace Foo
{
    class Bar
    {
        public function getId()
        {
        }
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