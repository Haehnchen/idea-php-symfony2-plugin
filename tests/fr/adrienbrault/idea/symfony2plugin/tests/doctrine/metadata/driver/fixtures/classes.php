<?php

namespace Doctrine\ORM\Mapping {
    class Entity {};
    class Table {};
    class Column {};
    class OneToMany {};
    class OneToOne {};
    class ManyToOne {};
    class ManyToMany {};
}

namespace TYPO3\Flow\Annotations {
    class Entity {};
}

namespace Doctrine\Orm {

    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @ORM\Entity()
     * @ORM\Table(name="FOO")
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

namespace Doctrine\Flow\Orm {

    use TYPO3\Flow\Annotations AS FLOW3;
    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @FLOW3\Entity()
     */
    class Annotation {

        /**
         * @ORM\Column(type="string")
         */
        private $email;

        /**
         * @var \DateTime
         *
         * @ORM\ManyToMany()
         */
        public $car;
    }
}
