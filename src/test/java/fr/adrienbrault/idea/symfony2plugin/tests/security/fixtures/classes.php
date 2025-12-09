<?php

namespace Symfony\Component\Security\Core\Authorization
{
    interface AuthorizationCheckerInterface
    {
        public function isGranted($attributes, $object = null);
    }

    interface UserAuthorizationCheckerInterface
    {
        public function isGrantedForUser(\Symfony\Component\Security\Core\User\UserInterface $user, $attribute, $subject = null);
    }
}

namespace Symfony\Component\Security\Core\User
{
    interface UserInterface
    {
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