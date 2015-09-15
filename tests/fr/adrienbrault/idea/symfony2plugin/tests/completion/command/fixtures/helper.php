<?php

namespace Symfony\Component\Console\Helper
{
    interface HelperInterface
    {
        public function getName();
    }

    interface HelperSet
    {
        public function get();
        public function has();
    }
}


namespace Symfony\Component\Console\Command
{
    class Command
    {
        public function getHelper(){}
    }
}


namespace
{
    use Symfony\Component\Console\Helper\HelperInterface;

    class FooHelper implements HelperInterface
    {
        public function getName()
        {
            return 'foo';
        }
    }
}

