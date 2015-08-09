<?php

namespace Doctrine\DBAL\Types {
    interface Type {
        const FOO = "foo_const";
        public function getName();
    }
}

namespace {

    use Doctrine\DBAL\Types\Type;

    class Foo implements Type {
        public function getName() {
            return Doctrine\DBAL\Types\Type::FOO;
        }
    }

    class Bar implements Type {
        public function getName() {
            return "BAR";
        }
    }
}
