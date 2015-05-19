<?php

namespace {{ ns }};

use Symfony\Bundle\FrameworkBundle\Controller\Controller;

class {{ class }} extends Controller
{
    public function indexAction($name)
    {
        return $this->render('', array('name' => $name));
    }
}
