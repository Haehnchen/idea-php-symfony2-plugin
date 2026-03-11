<?php

namespace Doctrine\ORM\Mapping {
    class Entity {}
    class Table {}
    class Column {}
}

namespace App\Entity {
    use Doctrine\ORM\Mapping AS ORM;

    #[ORM\Entity]
    #[ORM\Table(name: "php_attribute_table")]
    class AttributeUser
    {
        #[ORM\Column(type: "integer")]
        private int $id;

        #[ORM\Column(type: "string")]
        private string $name;
    }
}
