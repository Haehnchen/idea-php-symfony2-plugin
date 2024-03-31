<?php

declare(strict_types=1);

namespace {{ namespace }};

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class {{ class }} extends AbstractController
{

    /**
     * @Route("{{ path }}")
     */
    public function index(): Response
    {
        return $this->render('{{ template_path }}/index.html.twig');
    }
}
