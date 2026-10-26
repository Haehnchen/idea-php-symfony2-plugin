<?php

namespace Symfony\Component\Form {
    interface FormBuilderInterface { public function add($name, $type = null, array $options = []); }
}

namespace Symfony\Component\Form\Extension\Core\Type {
    class CollectionType implements \Symfony\Component\Form\FormTypeInterface {}
    class RepeatedType implements \Symfony\Component\Form\FormTypeInterface {}
}

namespace App\Form {
    use Symfony\Component\Form\Attribute\AsFormType;
    use Symfony\Component\Form\Attribute\FormField;
    use Symfony\Component\Form\Extension\Core\Type\CollectionType;
    use Symfony\Component\Form\Extension\Core\Type\RepeatedType;

    #[AsFormType]
    class AddressData {
        #[FormField]
        public string $street;

        #[FormField(name: 'postalCode')]
        private string $zip;
    }

    #[AsFormType]
    class NestedData {
        #[FormField(AddressData::class)]
        public AddressData $address;

        #[FormField(CollectionType::class, options: ['entry_type' => AddressData::class, 'allow_add' => true])]
        public array $addresses;

        #[FormField(CollectionType::class, options: ['entry_type' => AddressData::class, 'allow_add' => true, 'prototype' => false])]
        public array $fixedAddresses;

        #[FormField(RepeatedType::class, options: ['type' => AddressData::class, 'first_name' => 'original', 'second_name' => 'confirmation'])]
        public AddressData $repeated;

        #[FormField(RepeatedType::class)]
        public string $password;

        #[FormField(CollectionType::class, options: [
            'entry_type' => CollectionType::class,
            'entry_options' => ['entry_type' => AddressData::class, 'allow_add' => true],
            'allow_add' => true,
        ])]
        public array $groups;
    }

    class ClassicNestedType implements \Symfony\Component\Form\FormTypeInterface {
        public function buildForm(\Symfony\Component\Form\FormBuilderInterface $builder, array $options) {
            $builder->add('address', AddressData::class);
            $builder->add('addresses', CollectionType::class, ['entry_type' => AddressData::class, 'allow_add' => true]);
        }
    }
}

namespace App\Controller {
    class NestedFormController extends \Symfony\Bundle\FrameworkBundle\Controller\AbstractController {
        public function index() {
            $form = $this->createForm(\App\Form\NestedData::class);
            return $this->render('form/nested.html.twig', ['form' => $form->createView()]);
        }

        public function classic() {
            $form = $this->createForm(\App\Form\ClassicNestedType::class);
            return $this->render('form/nested_classic.html.twig', ['form' => $form->createView()]);
        }
    }
}
