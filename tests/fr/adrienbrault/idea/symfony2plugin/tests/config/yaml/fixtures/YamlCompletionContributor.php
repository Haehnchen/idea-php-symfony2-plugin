<?php

namespace Symfony\Component\HttpKernel\Bundle{
    interface Bundle {}
}

namespace FooBundle {
    class FooBundle implements \Symfony\Component\HttpKernel\Bundle\Bundle {}
}

namespace FooBundle\Controller {
    class FooController {
        public function fooAction() {}
    }
}