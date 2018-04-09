<?php

namespace Doctrine\ORM\Mapping {
    class Entity {};
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

    /**
     * @ORM\Entity(repositoryClass="Foo")
     */
    class Annotation {};

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
