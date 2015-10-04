<?php

namespace Doctrine\ORM\Mapping {
    class Entity {};
    class Column {};
    class OneToMany {};
    class OneToOne {};
    class ManyToOne {};
    class ManyToMany {};
}

namespace Doctrine\Orm {

    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @ORM\Entity()
     */
    class Annotation {

        /**
         * @ORM\Column(type="string")
         */
        private $email;

        /**
         * @ORM\OneToMany(targetEntity="Phonenumber")
         */
        public $phonenumbers;

        /**
         * @ORM\OneToOne(targetEntity="Address")
         */
        public $address;

        /**
         * @ORM\ManyToOne(targetEntity="Apple")
         */
        public $apple;

        /**
         * @ORM\ManyToMany(targetEntity="Egg")
         */
        public $egg;
    };

}
