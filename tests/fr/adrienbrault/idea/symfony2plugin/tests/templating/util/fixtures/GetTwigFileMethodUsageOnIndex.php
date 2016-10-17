<?php

namespace Template\Bar
{
    class MyTemplate
    {
        public function fooAction()
        {
            $foo = new \stdClass();
            $foo->render('car.html.twig');
        }
    }
}

namespace
{
    function foo() {
        $foo = new \stdClass();
        $foo->render('car.html.twig');
    }
}
