<?php

namespace Doctrine\ODM\MongoDB\Types {
    abstract class Type{};

    abstract class StringType extends Type {}
    abstract class FooType extends Type {
        function getName() {
            return 'mongodb_foo_bar';
        }
    }
}

namespace Doctrine\ODM\CouchDB\Types {
    abstract class Type{};

    abstract class StringType extends Type {}
    abstract class FooType extends Type {
        function getName() {
            return 'couchdb_foo_bar';
        }
    }
}

namespace Doctrine\DBAL\Types {
    abstract class Type{};

    abstract class StringType extends Type {}
    abstract class FooType extends Type {
        function getName() {
            return 'orm_foo_bar';
        }
    }
}

namespace Doctrine\Property {
    class Fields{
        private $id;
        private $name;
        const FOO = '';
    };
}