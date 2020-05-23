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

namespace Doctrine\Orm\MyTrait {
    use Doctrine\ORM\Mapping AS ORM;
    use Doctrine\Apple;

    trait EntityTrait
    {
        /**
         * @ORM\ManyToOne(targetEntity=Apple::class)
         */
        public $appleTrait;

        /**
         * @ORM\Column(type="string")
         */
        private $emailTrait;
    }
}

namespace Doctrine\Orm\MyExtends {
    use Doctrine\ORM\Mapping AS ORM;
    use Doctrine\FooBar;

    abstract class EntityExtends
    {
        /**
         * @ORM\ManyToOne(targetEntity=FooBar::class)
         */
        public $appleExtends;
    }
}

namespace Doctrine\Orm {

    use Doctrine\ORM\Mapping AS ORM;
    use Doctrine\Egg as SelfAlias;
    use Doctrine\Egg;
    use Doctrine\Orm\MyExtends\EntityExtends;
    use Doctrine\Orm\MyTrait\EntityTrait;

    /**
     * @ORM\Entity()
     * @ORM\Table(name="FOO")
     */
    class Annotation extends EntityExtends {
        use EntityTrait;

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

        /**
         * @ORM\ManyToMany(targetEntity=Egg::class)
         */
        public $eggClass;

        /**
         * @ORM\ManyToMany(targetEntity=SelfAlias::class)
         */
        public $eggSelfAlias;

        /**
         * @ORM\ManyToMany(targetEntity=SelfAlias\Foo\Bar::class)
         */
        public $eggSelfAliasFooBar;
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
