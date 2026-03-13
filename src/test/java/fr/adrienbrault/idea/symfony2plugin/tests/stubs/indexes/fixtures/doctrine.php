<?php

namespace Doctrine\ORM\Mapping {
    class Entity {};
    class Table {};
}

namespace TYPO3\Flow\Annotations {
    class Entity {};
}

namespace Doctrine\ODM\MongoDB\Mapping\Annotations {
    class Document {};
}

namespace Doctrine\ODM\CouchDB\Mapping\Annotations {
    class Document {};
}

namespace Doctrine\Orm {

    use Doctrine\ORM\Mapping AS ORM;
    use Doctrine\OrmRepository\AttributeEntityRepository;

    /**
     * @ORM\Entity(repositoryClass="Foo")
     */
    class Annotation {}

    /**
     * @ORM\Entity(repositoryClass="Foo")
     * @ORM\Table(name="annotation_users")
     */
    class AnnotationWithTable {}

    #[ORM\Entity(repositoryClass: AttributeEntityRepository::class)]
    class AttributeEntity {}

    #[ORM\Entity(repositoryClass: AttributeEntityRepository::class)]
    #[ORM\Table(name: "attribute_users")]
    class AttributeEntityWithTable {}
}

namespace Doctrine\OrmRepository {
    class AttributeEntityRepository {};
}

namespace Doctrine\Flow\Orm {

    use TYPO3\Flow\Annotations AS FLOW3;
    use Doctrine\ORM\Mapping AS ORM;

    /**
     * @FLOW3\Entity()
     */
    class Annotation {}
}

namespace Doctrine\MongoDB {

    use Doctrine\ODM\MongoDB\Mapping\Annotations;

    /**
     * @Document()
     */
    class Annotation {}
}

namespace Doctrine\CouchDB {

    use Doctrine\ODM\CouchDB\Mapping\Annotations;

    /**
     * @Document()
     */
    class Annotation {}
}
