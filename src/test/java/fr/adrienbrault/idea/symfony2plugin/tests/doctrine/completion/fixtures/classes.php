<?php

namespace Doctrine\Common\Persistence
{
    interface ObjectManager
    {
        public function getRepository();
    }

    interface ManagerRegistry
    {
        public function getRepository();
    }
}


namespace My\Model
{
    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @ORM\Entity()
     */
    class FooModel
    {
    }
}