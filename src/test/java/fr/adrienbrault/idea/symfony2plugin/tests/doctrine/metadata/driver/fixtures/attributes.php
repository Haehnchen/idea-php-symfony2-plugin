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

    interface Countable {}
    interface Serializable {}

    enum Status: string
    {
        case DRAFT = 'draft';
        case PUBLISHED = 'published';
    }
}

namespace ORM\Attributes {
    use Doctrine\ORM\Mapping AS ORM;
    use ORM\Foobar\Egg;
    use ORM\Foobar\Status;
    use ORM\Foobar\Countable;
    use ORM\Foobar\Serializable;
    use Doctrine\Orm\MyTrait\EntityTrait;

    #[ORM\Entity]
    #[ORM\Table(name: "table_name", schema: "schema_name")]
    class AttributeEntity {
        use EntityTrait;

        #[ORM\Column(type: "string", length: 32, unique: true, nullable: false)]
        private $email;

        #[ORM\Column(type: "decimal", precision: 10, scale: 2, nullable: true)]
        private string $price;

        #[ORM\Column(type: "string", enumType: Status::class)]
        private Status $status;

        #[ORM\OneToMany(targetEntity: Egg::class)]
        public $phonenumbers;

        #[ORM\OneToOne(targetEntity: Egg::class)]
        public $address;

        #[ORM\ManyToOne(targetEntity: Egg::class)]
        public $apple;

        #[ORM\ManyToMany(targetEntity:Egg::class)]
        public $egg;

        #[ORM\ManyToMany(targetEntity: '\ORM\Foobar\Egg')]
        public $eggClassString;

        #[ORM\ManyToMany(targetEntity: 'ORM\Foobar\Egg')]
        public $eggClassStringBackslashless;

        #[ORM\ManyToMany]
        public null|Egg $eggTargetEntity;

        #[ORM\Column(type: "integer")]
        private int|string $count;

        #[ORM\Column(type: "string")]
        private null|Egg $optionalEgg;

        #[ORM\Column(type: "string")]
        private Countable&Serializable $taggedItem;
    };
}