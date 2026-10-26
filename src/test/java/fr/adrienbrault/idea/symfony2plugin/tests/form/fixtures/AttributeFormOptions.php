<?php

namespace Symfony\Component\Form\Attribute {
    #[\Attribute(\Attribute::TARGET_PROPERTY)]
    class FormField {
        public function __construct(public ?string $type = null, public array $options = [], public ?string $name = null) {}
    }
}

namespace Symfony\Bundle\FrameworkBundle\Controller {
    class AbstractController {
        public function createForm($type, $data = null, array $options = []) {}
    }
}

namespace App {
    use Symfony\Component\Form\Attribute\AsFormType;
    use Symfony\Component\OptionsResolver\OptionsResolver;

    #[AsFormType(options: ['own_option' => true, 'translation_domain' => 'foo'])]
    class AccountData extends IntermediateAccountData {
        public string $email;
        public function setDisplayName(string $name) {}

        // DTO methods are not form hooks.
        public function configureOptions(OptionsResolver $resolver) {
            $resolver->setDefaults(['data_class' => OtherData::class, 'unused_dto_option' => true]);
        }
    }

    class OtherData {}
}
