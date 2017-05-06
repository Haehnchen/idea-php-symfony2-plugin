<?php

namespace Foo
{
    class Bar
    {
        public function getId()
        {
        }
    }
}


namespace Doctrine\Common\Persistence
{
    interface ObjectManager
    {
        public function find($className, $id);
    }
}