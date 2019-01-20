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

    class BarRepository implements ObjectRepository {
        public function bar() {}
    }

    class WrongClass {}

    class BarController {
        public function getRepo($name): BarRepository {}
        public function getBoo($name): WrongClass {}
    }
}

namespace Doctrine\Common\Persistence
{
    interface ObjectRepository
    {
        public function find();
    }
}