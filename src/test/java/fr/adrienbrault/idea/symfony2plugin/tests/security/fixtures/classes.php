<?php

namespace Symfony\Component\Security\Core\Authorization
{
    interface AuthorizationCheckerInterface
    {
        public function isGranted($attributes, $object = null);
    }
}

namespace Sensio\Bundle\FrameworkExtraBundle\Configuration
{
    /**
     * @Annotation
     */
    class Security
    {
    }

    /**
     * @Annotation
     */
    class IsGranted
    {
    }
}

namespace Symfony\Component\Security\Http\Attribute {
    class IsGranted
    {
    }
}