<?php

namespace Doctrine\Common\Persistence {
    interface ObjectRepository {};
}

namespace Foo\Bar {
    use Doctrine\Common\Persistence\ObjectRepository;
    abstract class BarRepository implements ObjectRepository { }
}