<?php

namespace Symfony\Component\Form {
    interface FormTypeInterface {}
    class FormView {}
    interface FormInterface {
        public function createView(): FormView;
        public function get(string $name): FormInterface;
        public function has(string $name): bool;
    }
    interface FormFactoryInterface {
        public function create($type, $data = null, array $options = []): FormInterface;
        public function createNamed($name, $type, $data = null, array $options = []): FormInterface;
    }
}

namespace Symfony\Component\Form\Extension\Core\Type {
    class TextType implements \Symfony\Component\Form\FormTypeInterface {}
}

namespace Symfony\Bundle\FrameworkBundle\Controller {
    abstract class AbstractController {
        public function createForm($type): \Symfony\Component\Form\FormInterface {}
        public function render($template, array $parameters = []) {}
    }
}

namespace App\Form {
    use Symfony\Component\Form\Attribute\AsFormType;
    use Symfony\Component\Form\Attribute\FormField;
    use Symfony\Component\Form\Extension\Core\Type\TextType;

    #[AsFormType]
    abstract class ContactData {
        #[FormField]
        public string $identifier;
    }

    #[AsFormType]
    class AccountData extends ContactData {
        #[FormField(TextType::class)]
        public string $email;

        #[FormField(name: 'displayName')]
        private string $internalName;

        public string $unannotated;
    }
}

namespace App\Controller {
    class AccountController extends \Symfony\Bundle\FrameworkBundle\Controller\AbstractController {
        public function index() {
            $form = $this->createForm(\App\Form\AccountData::class);

            return $this->render('form/attributes.html.twig', ['form' => $form->createView()]);
        }
    }
}
