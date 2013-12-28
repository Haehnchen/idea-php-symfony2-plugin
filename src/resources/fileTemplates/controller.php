<?php

namespace {{ Namespace }};

use Symfony\Bundle\FrameworkBundle\Controller\Controller;

class {{ ControllerName }}Controller extends Controller
{
    public function indexAction($name)
    {
        return $this->render('', array('name' => $name));
    }
}
