<?php

namespace Doctrine\Common\Persistence
{
    interface ManagerRegistry
    {
        public function getRepository();
    }
    interface ObjectManager
    {
        public function getRepository();
        public function find();
    }
}
