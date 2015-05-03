<?php

namespace {{ ns }};

use Symfony\Bundle\FrameworkBundle\Controller\Controller;

class {{ class }}Controller extends Controller
{
    public function indexAction($name)
    {
        return $this->render('', array('name' => $name));
    }
}