<?php

namespace Doctrine\ORM\Mapping {
    class Entity {
        public function __construct(...$args) {}
    }

    class Table {
        public function __construct(...$args) {}
    }

    class Id {
        public function __construct(...$args) {}
    }

    class Column {
        public function __construct(...$args) {}
    }
}

namespace Entity {
    use Doctrine\ORM\Mapping as ORM;

    /**
     * @ORM\Entity
     * @ORM\Table(name="annotated_users")
     */
    class AnnotatedUser
    {
        /**
         * @ORM\Id
         * @ORM\Column(type="integer")
         */
        private $id;
    }

    #[ORM\Entity]
    #[ORM\Table(name: 'attribute_users')]
    class AttributeUser
    {
        #[ORM\Id]
        #[ORM\Column(type: 'integer')]
        private int $id;
    }

    class BlogPost
    {
    }

    class XmlMappedUser
    {
    }

    class YamlMappedUser
    {
    }

    class FooBar
    {
    }
}
