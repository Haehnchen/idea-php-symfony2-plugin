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

namespace Doctrine\Orm\MyTrait {
    use Doctrine\ORM\Mapping AS ORM;

    trait EntityTrait
    {
        #[ORM\ManyToOne(targetEntity: Egg::class)]
        public $appleTrait;

        #[ORM\Column(type: "string")]
        private $emailTrait;
    }
}

namespace ORM\Foobar {
    class Egg {}
}

namespace ORM\Attributes {
    use Doctrine\ORM\Mapping AS ORM;
    use ORM\Foobar\Egg;
    use Doctrine\Orm\MyTrait\EntityTrait;

    #[ORM\Entity]
    #[ORM\Table(name: "table_name", schema: "schema_name")]
    class AttributeEntity {
        use EntityTrait;

        #[ORM\Column(type: "string", length: 32, unique: true, nullable: false)]
        private $email;

        #[ORM\OneToMany(targetEntity: Egg::class)]
        public $phonenumbers;

        #[ORM\OneToOne(targetEntity: Egg::class)]
        public $address;

        #[ORM\ManyToOne(targetEntity: Egg::class)]
        public $apple;

        #[ORM\ManyToMany(targetEntity:Egg::class)]
        public $egg;
    };
}