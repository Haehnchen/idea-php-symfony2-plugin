<?php

namespace Doctrine\Common\Persistence
{
    interface ObjectManager
    {
        public function getRepository();
    }

    interface ObjectFoo
    {
        public function getRepository();
    }

}

namespace PhpType
{
    class Foo {}

    class Bar {
        /**
         * @return Foo
         */
        public function foo() {}

        public function bar() {}
    }
}
