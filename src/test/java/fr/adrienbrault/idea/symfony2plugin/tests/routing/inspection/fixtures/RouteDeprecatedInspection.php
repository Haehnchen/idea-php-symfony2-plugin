<?php

namespace App\Controller
{
    use Symfony\Component\Routing\Attribute\Route;

    class RouteDeprecatedController
    {
        /**
         * @deprecated
         */
        #[Route('/deprecated', name: 'deprecated_route')]
        public function deprecatedAction() {}

        #[Route('/active', name: 'active_route')]
        public function activeAction() {}
    }
}
