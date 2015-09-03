<?php

namespace Doctrine\Common\Persistence {
    interface ObjectManager {
        function getRepository($foo);
    };
}

namespace Foo {
    use Doctrine\Common\Persistence\ObjectManager;

    abstract class Bar implements ObjectManager {}
    abstract class BarRepository implements ObjectManager {
        public function bar() {}
    }
}