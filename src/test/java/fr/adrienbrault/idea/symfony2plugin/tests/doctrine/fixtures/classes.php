<?php

namespace Doctrine\Common\Persistence {
    interface ObjectManager {

        /**
         * @return \Doctrine\Common\Persistence\ObjectRepository
         */
        function getRepository();
    };
}

namespace Foo {

    use Doctrine\Common\Persistence\ObjectRepository;

    abstract class Bar implements ObjectRepository {}

    class BarRepository {
        public function bar() {}
    }
}

namespace Doctrine\Common\Persistence
{
    interface ObjectRepository
    {
        public function find();
    }
}