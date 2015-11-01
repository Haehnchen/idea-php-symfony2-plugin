<?php

namespace Doctrine\Tests\ORM\Mapping {
    class YamlUser {}
    class YamlUserRepository {}
}

namespace Foo {
    class Bar {}

    class Car {};

    class Repository {};
}

namespace Foo\Bar\Repository {

    use Doctrine\Common\Persistence\ObjectRepository;

    abstract class FooBarRepository implements ObjectRepository {}
}

namespace Doctrine\Common\Persistence {
    interface ObjectRepository {};
}

namespace Entity {
    class Bar{};
    class BarRepository{};
}