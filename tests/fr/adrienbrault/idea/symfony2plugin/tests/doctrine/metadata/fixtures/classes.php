<?php
namespace
{
    class MyDateTime {}
}

namespace Doctrine\Common\Persistence {
    interface ObjectRepository{};
}

namespace Foo\Bar\Ns {

    use Doctrine\Common\Persistence\ObjectRepository;

    class Bar{};
    class BarRepo implements ObjectRepository{};
}
