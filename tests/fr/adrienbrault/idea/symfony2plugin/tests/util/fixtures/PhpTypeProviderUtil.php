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
