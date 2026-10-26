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

    trait ContactFields {
        #[FormField(name: 'displayName')]
        private string $traitName;

        #[FormField(TextType::class)]
        public string $traitEmail;
    }

    trait NestedContactFields {
        use ContactFields;
    }

    /** @property string $traitEmail */
    #[AsFormType]
    class TraitAccountData {
        use NestedContactFields;
    }

    #[AsFormType]
    class ChildTraitAccountData extends TraitAccountData {}

    #[AsFormType]
    class OtherTraitData {
        use ContactFields;
    }

    #[AsFormType]
    class ShadowedTraitData {
        use ContactFields;

        private string $traitName;
    }

    class PlainTraitParent {
        use ContactFields;
    }

    #[AsFormType]
    class PlainParentTraitData extends PlainTraitParent {}
}

namespace App\Controller {
    class AccountController extends \Symfony\Bundle\FrameworkBundle\Controller\AbstractController {
        public function index() {
            $form = $this->createForm(\App\Form\AccountData::class);

            return $this->render('form/attributes.html.twig', ['form' => $form->createView()]);
        }
    }

    class TraitFormController extends \Symfony\Bundle\FrameworkBundle\Controller\AbstractController {
        public function index() {
            $form = $this->createForm(\App\Form\TraitAccountData::class);
            return $this->render('form/traits.html.twig', ['form' => $form->createView()]);
        }

        public function other() {
            $form = $this->createForm(\App\Form\OtherTraitData::class);
            return $this->render('form/other_traits.html.twig', ['form' => $form->createView()]);
        }

        public function shadowed() {
            $form = $this->createForm(\App\Form\ShadowedTraitData::class);
            return $this->render('form/shadowed_traits.html.twig', ['form' => $form->createView()]);
        }
    }
}
