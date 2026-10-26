<?php

namespace Symfony\Component\Form\Attribute {
    #[\Attribute(\Attribute::TARGET_PROPERTY)]
    class FormField {
        public function __construct(
            public ?string $type = null,
            public array $options = [],
            public ?string $name = null,
        ) {}
    }
}

namespace Symfony\Component\Form\Extension\Core\Type {
    class EmailType extends \Symfony\Component\Form\AbstractType {
        public function configureOptions($resolver) {
            $resolver->setDefaults([
                'required' => true,
                'email_option' => null,
            ]);
        }

        public function getParent() {
            return FormType::class;
        }
    }
}

namespace Foo\Form {
    class EmailTypeExtension implements \Symfony\Component\Form\FormTypeExtensionInterface {
        public function getExtendedTypes() {
            return [\Symfony\Component\Form\Extension\Core\Type\EmailType::class];
        }

        public function configureOptions($resolver) {
            $resolver->setDefaults(['email_extension' => null]);
        }
    }
}
