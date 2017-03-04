<?php

namespace Symfony\Component\Security\Core\Authorization
{
    interface AuthorizationCheckerInterface
    {
        public function isGranted($attributes, $object = null);
    }
}