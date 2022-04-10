<?php

namespace Drupal\Foo\Entity
{

    /**
     * Defines the contact form entity.
     *
     * @ConfigEntityType(
     *   id = "contact_form",
     * )
     */
    class ContactForm
    {
    }
}

namespace Drupal\Core\Controller
{
    abstract class ControllerBase {};
}

namespace Drupal\contact\Controller {

    use Drupal\Core\Controller\ControllerBase;

    class ContactController extends ControllerBase {
        public function foo() {}
        private function privateBar() {}
    }
}
